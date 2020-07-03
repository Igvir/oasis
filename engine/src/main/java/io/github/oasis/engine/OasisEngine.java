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

package io.github.oasis.engine;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import io.github.oasis.core.Event;
import io.github.oasis.core.configs.OasisConfigs;
import io.github.oasis.core.elements.ElementModuleFactory;
import io.github.oasis.core.exception.OasisException;
import io.github.oasis.core.external.Db;
import io.github.oasis.core.external.EventStreamFactory;
import io.github.oasis.core.external.SourceFunction;
import io.github.oasis.core.external.messages.OasisCommand;
import io.github.oasis.core.external.messages.PersistedDef;
import io.github.oasis.db.redis.RedisDb;
import io.github.oasis.engine.actors.ActorNames;
import io.github.oasis.engine.actors.OasisSupervisor;
import io.github.oasis.engine.element.points.PointsModuleFactory;
import io.github.oasis.engine.ext.ExternalParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * @author Isuru Weerarathna
 */
public class OasisEngine implements SourceFunction {

    private static final Logger LOG = LoggerFactory.getLogger(OasisEngine.class);

    private ActorSystem oasisEngine;
    private ActorRef supervisor;

    private EngineContext context;

    public OasisEngine(EngineContext context) {
        this.context = context;
    }

    public void start() throws OasisException {
        context.init();

        String engineName = context.getConfigs().getEngineName();
        oasisEngine = ActorSystem.create(engineName, context.getConfigs().getConfigRef());
        supervisor = oasisEngine.actorOf(Props.create(OasisSupervisor.class, context), ActorNames.OASIS_SUPERVISOR);
        LOG.info("Oasis engine initialization invoked...");

        LOG.info("Bootstrapping event stream...");
        bootstrapEventStream(oasisEngine);
    }

    private void bootstrapEventStream(ActorSystem system) throws OasisException {
        EventStreamFactory streamFactory = ExternalParty.EXTERNAL_PARTY.get(oasisEngine).getStreamFactory();
        try {
            streamFactory.getEngineEventSource().init(context, this);
        } catch (Exception e) {
            LOG.error("Error initializing event stream! Shutting down engine...", e);
            system.stop(ActorRef.noSender());
            throw new OasisException(e.getMessage(), e);
        }
    }

    @Override
    public void submit(PersistedDef dto) {
        Object message = DtoHandler.derive(dto, context);
        if (message != null) {
            supervisor.tell(message, supervisor);
        }
    }

    @Override
    public void submit(PersistedDef dto, AckCallback callback) {
        Object message = DtoHandler.derive(dto, context);
        if (message != null) {
            supervisor.tell(message, supervisor);
            callback.accepted();
        } else {
            callback.rejected();
        }
    }

    @Override
    public void submit(OasisCommand command) {
        supervisor.tell(command, supervisor);
    }

    @Override
    public void submit(Event event) {
        supervisor.tell(event, supervisor);
    }

    public void submit(Object message) {
        supervisor.tell(message, supervisor);
    }

    public void submitAll(Object... events) {
        for (Object event : events) {
            submit(event);
        }
    }
}