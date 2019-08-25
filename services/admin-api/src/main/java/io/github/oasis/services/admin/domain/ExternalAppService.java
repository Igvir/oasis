/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.oasis.services.admin.domain;

import io.github.oasis.services.admin.internal.dao.IExternalAppDao;
import io.github.oasis.services.admin.internal.dto.NewAppDto;
import io.github.oasis.services.admin.json.apps.ApplicationAddedJson;
import io.github.oasis.services.admin.json.apps.NewApplicationJson;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author Isuru Weerarathna
 */
@Component
public class ExternalAppService {

    private static final int RSA_KEY_SIZE = 2048;
    private static final String RSA = "RSA";

    private IExternalAppDao externalAppDao;

    public ExternalAppService(IExternalAppDao externalAppDao) {
        this.externalAppDao = externalAppDao;
    }

    public ApplicationAddedJson addApplication(NewApplicationJson newApplication) {
        NewAppDto appDto = NewAppDto.from(newApplication);
        assignKeys(appDto);

        String token = appDto.getToken();
        int id = externalAppDao.addApplication(appDto);

        

        return new ApplicationAddedJson(id, token);
    }

    public NewAppDto assignKeys(NewAppDto appDto) {
        try {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
            SecureRandom secureRandom = new SecureRandom(appDto.getName().getBytes(StandardCharsets.UTF_8));
            keyGen.initialize(RSA_KEY_SIZE, secureRandom);
            KeyPair keyPair = keyGen.generateKeyPair();
            return appDto.assignKeys(keyPair);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate keys for the external application!");
        }
    }

}
