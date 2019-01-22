package io.github.isuru.oasis.game.persist.rabbit;

import io.github.isuru.oasis.model.configs.ConfigKeys;
import io.github.isuru.oasis.model.configs.Configs;
import io.github.isuru.oasis.model.configs.EnvKeys;
import org.apache.flink.streaming.connectors.rabbitmq.common.RMQConnectionConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ClearSystemProperties;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Assertions;

import java.util.Properties;

public class RabbitUtilsTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public final ClearSystemProperties clearRabbitHost = new ClearSystemProperties(ConfigKeys.KEY_RABBIT_HOST);

    @Test
    public void testCreateRabbitSourceConfig() {
        environmentVariables.set(EnvKeys.OASIS_RABBIT_HOST, "envHost");
        Assertions.assertEquals("envHost", System.getenv(EnvKeys.OASIS_RABBIT_HOST));

        {
            Assertions.assertThrows(IllegalStateException.class,
                    () -> RabbitUtils.createRabbitSourceConfig(Configs.create()));

            {
                Properties props = createProps(ConfigKeys.KEY_RABBIT_GSRC_USERNAME, "rabbituser");
                Assertions.assertThrows(IllegalStateException.class,
                        () -> RabbitUtils.createRabbitSourceConfig(Configs.from(props)));
            }
            {
                Properties props = createProps(ConfigKeys.KEY_RABBIT_GSRC_PASSWORD, "rabbitpass");
                Assertions.assertThrows(IllegalStateException.class,
                        () -> RabbitUtils.createRabbitSourceConfig(Configs.from(props)));
            }
        }

        {
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSRC_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSRC_PASSWORD, "rabbitpass");
            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSourceConfig(Configs.from(props));
            Assertions.assertEquals("envHost", sourceConfig.getHost());
            Assertions.assertEquals(ConfigKeys.DEF_RABBIT_VIRTUAL_HOST, sourceConfig.getVirtualHost());
            Assertions.assertEquals(ConfigKeys.DEF_RABBIT_PORT, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_PASSWORD), sourceConfig.getPassword());
        }

        {
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSRC_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSRC_PASSWORD, "rabbitpass");
            props.setProperty(ConfigKeys.KEY_RABBIT_PORT, "1234");
            props.setProperty(ConfigKeys.KEY_RABBIT_VIRTUAL_HOST, "testOasis");

            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSourceConfig(Configs.from(props));
            Assertions.assertEquals("envHost", sourceConfig.getHost());
            Assertions.assertEquals("testOasis", sourceConfig.getVirtualHost());
            Assertions.assertEquals(1234, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_PASSWORD), sourceConfig.getPassword());
        }

        {
            System.setProperty(ConfigKeys.KEY_RABBIT_HOST, "overriddenHost");
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSRC_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSRC_PASSWORD, "rabbitpass");
            props.setProperty(ConfigKeys.KEY_RABBIT_HOST, "myhost");
            props.setProperty(ConfigKeys.KEY_RABBIT_PORT, "1234");
            props.setProperty(ConfigKeys.KEY_RABBIT_VIRTUAL_HOST, "testOasis");

            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSourceConfig(Configs.from(props));
            Assertions.assertEquals("envHost", sourceConfig.getHost());
            Assertions.assertEquals("testOasis", sourceConfig.getVirtualHost());
            Assertions.assertEquals(1234, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_PASSWORD), sourceConfig.getPassword());
        }

    }

    @Test
    public void testCreateRabbitSourceConfigOverride() {
        {
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSRC_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSRC_PASSWORD, "rabbitpass");
            props.setProperty(ConfigKeys.KEY_RABBIT_HOST, "myhost");
            props.setProperty(ConfigKeys.KEY_RABBIT_PORT, "1234");
            props.setProperty(ConfigKeys.KEY_RABBIT_VIRTUAL_HOST, "testOasis");

            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSourceConfig(Configs.from(props));
            Assertions.assertEquals("myhost", sourceConfig.getHost());
            Assertions.assertEquals("testOasis", sourceConfig.getVirtualHost());
            Assertions.assertEquals(1234, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSRC_PASSWORD), sourceConfig.getPassword());
        }
    }


    @Test
    public void testCreateRabbitSinkConfig() {
        environmentVariables.set(EnvKeys.OASIS_RABBIT_HOST, "envHost");
        Assertions.assertEquals("envHost", System.getenv(EnvKeys.OASIS_RABBIT_HOST));

        {
            Assertions.assertThrows(IllegalStateException.class,
                    () -> RabbitUtils.createRabbitSinkConfig(Configs.create()));

            {
                Properties props = createProps(ConfigKeys.KEY_RABBIT_GSNK_USERNAME, "rabbituser");
                Assertions.assertThrows(IllegalStateException.class,
                        () -> RabbitUtils.createRabbitSinkConfig(Configs.from(props)));
            }
            {
                Properties props = createProps(ConfigKeys.KEY_RABBIT_GSNK_PASSWORD, "rabbitpass");
                Assertions.assertThrows(IllegalStateException.class,
                        () -> RabbitUtils.createRabbitSinkConfig(Configs.from(props)));
            }
        }

        {
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSNK_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSNK_PASSWORD, "rabbitpass");
            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSinkConfig(Configs.from(props));
            Assertions.assertEquals("envHost", sourceConfig.getHost());
            Assertions.assertEquals(ConfigKeys.DEF_RABBIT_VIRTUAL_HOST, sourceConfig.getVirtualHost());
            Assertions.assertEquals(ConfigKeys.DEF_RABBIT_PORT, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_PASSWORD), sourceConfig.getPassword());
        }

        {
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSNK_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSNK_PASSWORD, "rabbitpass");
            props.setProperty(ConfigKeys.KEY_RABBIT_PORT, "1234");
            props.setProperty(ConfigKeys.KEY_RABBIT_VIRTUAL_HOST, "testOasis");

            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSinkConfig(Configs.from(props));
            Assertions.assertEquals("envHost", sourceConfig.getHost());
            Assertions.assertEquals("testOasis", sourceConfig.getVirtualHost());
            Assertions.assertEquals(1234, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_PASSWORD), sourceConfig.getPassword());
        }

        {
            System.setProperty(ConfigKeys.KEY_RABBIT_HOST, "overriddenHost");
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSNK_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSNK_PASSWORD, "rabbitpass");
            props.setProperty(ConfigKeys.KEY_RABBIT_HOST, "myhost");
            props.setProperty(ConfigKeys.KEY_RABBIT_PORT, "1234");
            props.setProperty(ConfigKeys.KEY_RABBIT_VIRTUAL_HOST, "testOasis");

            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSinkConfig(Configs.from(props));
            Assertions.assertEquals("envHost", sourceConfig.getHost());
            Assertions.assertEquals("testOasis", sourceConfig.getVirtualHost());
            Assertions.assertEquals(1234, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_PASSWORD), sourceConfig.getPassword());
        }

    }

    @Test
    public void testCreateRabbitSinkConfigOverride() {
        {
            Properties props = createProps(
                    ConfigKeys.KEY_RABBIT_GSNK_USERNAME, "rabbituser",
                    ConfigKeys.KEY_RABBIT_GSNK_PASSWORD, "rabbitpass");
            props.setProperty(ConfigKeys.KEY_RABBIT_HOST, "myhost");
            props.setProperty(ConfigKeys.KEY_RABBIT_PORT, "1234");
            props.setProperty(ConfigKeys.KEY_RABBIT_VIRTUAL_HOST, "testOasis");

            RMQConnectionConfig sourceConfig = RabbitUtils.createRabbitSinkConfig(Configs.from(props));
            Assertions.assertEquals("myhost", sourceConfig.getHost());
            Assertions.assertEquals("testOasis", sourceConfig.getVirtualHost());
            Assertions.assertEquals(1234, sourceConfig.getPort());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_USERNAME), sourceConfig.getUsername());
            Assertions.assertEquals(props.getProperty(ConfigKeys.KEY_RABBIT_GSNK_PASSWORD), sourceConfig.getPassword());
        }
    }


    static Properties createProps(String k1, String v1) {
        return createProps(k1, v1, null, null);
    }

    static Properties createProps(String k1, String v1, String k2, String v2) {
        Properties properties = new Properties();
        properties.setProperty(k1, v1);
        if (k2 != null) properties.setProperty(k2, v2);
        return properties;
    }

}
