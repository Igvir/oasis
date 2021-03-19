/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.oasis.core.elements;

import io.github.oasis.core.elements.spec.BaseSpecification;
import io.github.oasis.core.external.messages.EngineMessage;
import org.yaml.snakeyaml.Yaml;

/**
 * @author Isuru Weerarathna
 */
public abstract class AbstractElementParser implements ElementParser {

    private final Yaml yaml = new Yaml();

    protected <T extends AbstractDef<? extends BaseSpecification>> T loadFrom(EngineMessage def, Class<T> clz) {
        System.out.println(def.getData());
        return yaml.loadAs(yaml.dump(def.getData()), clz);
    }

}
