package io.github.isuru.oasis.services.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.isuru.oasis.model.Event;
import io.github.isuru.oasis.model.collect.Pair;
import io.github.isuru.oasis.model.configs.Configs;
import io.github.isuru.oasis.services.exception.ApiAuthException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.Random;

/**
 * @author iweerarathna
 */
public final class AuthUtils {

    private static final String OASIS_ISSUER = "oasis";
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int RSA_KEY_SIZE = 2048;

    private MessageDigest digest;

    private Algorithm algorithm;
    private JWTVerifier verifier;
    private long expiryDate;

    private final ObjectMapper mapper = new ObjectMapper();

    private Configs configs;

    private AuthUtils() {}

    private static String toHexString(byte[] data) {
        Formatter result = new Formatter();
        for (byte b : data) {
            result.format("%02x", b);
        }
        return result.toString();
    }

    public static String generateHMAC(String data, PrivateKey key) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec signingKey = new SecretKeySpec(key.getEncoded(), HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));
    }

    public static Pair<PrivateKey, PublicKey> generateRSAKey(EventSourceToken sourceToken) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom secureRandom = new SecureRandom(sourceToken.getSourceName().getBytes());
        keyGen.initialize(RSA_KEY_SIZE, secureRandom);
        KeyPair keyPair = keyGen.generateKeyPair();
        return Pair.of(keyPair.getPrivate(), keyPair.getPublic());
    }

    public void init(Configs configs) throws Exception {
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);

        // init jwt configurations
        String publicKeyPath = configs.getStrReq("oasis.public.key");
        String privateKeyPath = configs.getStrReq("oasis.private.key");
        byte[] bytesPrivate = Files.readAllBytes(Paths.get(privateKeyPath));
        byte[] bytesPublic = Files.readAllBytes(Paths.get(publicKeyPath));
        PKCS8EncodedKeySpec specPrivate = new PKCS8EncodedKeySpec(bytesPrivate);
        X509EncodedKeySpec specPublic = new X509EncodedKeySpec(bytesPublic);
        RSAPrivateKey rsaPrivate = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(specPrivate);
        RSAPublicKey rsaPublic = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(specPublic);

        algorithm = Algorithm.RSA256(rsaPublic, rsaPrivate);
        verifier = JWT.require(algorithm)
                .withIssuer(OASIS_ISSUER)
                .build();

        expiryDate = LocalDate.of(2030, 12, 31)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public void ldapAuthUser(String username, String password) throws ApiAuthException {
        DirContext context = null;
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, configs.getStrReq("oasis.auth.ldap.url"));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);

        try {
            context = new InitialDirContext(env);
        } catch (NamingException e) {
            throw new ApiAuthException("User authentication failed! Username or password is incorrect!");
        } finally {
            try {
                if (context != null) {
                    context.close();
                }
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
    }

    public long getExpiryDate() {
        return expiryDate;
    }

    public Pair<String, Integer> issueSourceToken(EventSourceToken token) throws IOException {
        int nonce = RUtils.generateNonce();
        String text = token.getDisplayName() + String.valueOf(System.currentTimeMillis())
                + String.valueOf(nonce);
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Unable to generate a token for event source!", e);
            }
        }
        byte[] digest = this.digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return Pair.of(byteArrayToHexString(digest), nonce);
    }

    private static String byteArrayToHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte aB : b) {
            result.append(Integer.toString((aB & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }

    public String issueToken(TokenInfo tokenInfo) throws ApiAuthException {
        try {
            return JWT.create()
                    .withIssuer(OASIS_ISSUER)
                    .withExpiresAt(new Date(tokenInfo.getExp()))
                    .withClaim("user", tokenInfo.user)
                    .withClaim("role", tokenInfo.role)
                    .sign(algorithm);
        } catch (IllegalArgumentException | JWTCreationException e) {
            e.printStackTrace();
            throw new ApiAuthException("Unable to create login information for user " + tokenInfo.getUser() + "!");
        }
    }

    public TokenInfo verifyToken(String token) throws ApiAuthException {
        try {
            DecodedJWT jwt = verifier.verify(token);
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setUser(jwt.getClaim("user").asLong());
            tokenInfo.setRole(jwt.getClaim("role").asInt());
            tokenInfo.setExp(jwt.getExpiresAt().getTime());
            return tokenInfo;

        } catch (JWTVerificationException e) {
            throw new ApiAuthException("Provided token is invalid! It is either modified or expired! ["
                    + e.getClass().getSimpleName() + "]");
        }
    }

    public static AuthUtils get() {
        return Holder.INSTANCE;
    }

    public static class TokenInfo {
        private long user;
        private long exp;
        private int role = UserRole.PLAYER;

        public long getExp() {
            return exp;
        }

        public void setExp(long exp) {
            this.exp = exp;
        }

        public long getUser() {
            return user;
        }

        public void setUser(long user) {
            this.user = user;
        }

        public int getRole() {
            return role;
        }

        public void setRole(int role) {
            this.role = role;
        }
    }

    private static class Holder {
        private static final AuthUtils INSTANCE = new AuthUtils();
    }
}
