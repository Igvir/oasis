package io.github.isuru.oasis.unittest.utils;

import io.github.isuru.oasis.model.handlers.IStatesHandler;
import io.github.isuru.oasis.model.handlers.OStateNotification;
import org.apache.flink.api.java.tuple.Tuple5;

public class StatesCollector implements IStatesHandler {

    private String sinkId;

    public StatesCollector(String sinkId) {
        this.sinkId = sinkId;
    }

    @Override
    public void handleStateChange(OStateNotification stateNotification) {
        Memo.addState(sinkId,
                Tuple5.of(stateNotification.getUserId(),
                        (int)stateNotification.getStateRef().getId(),
                        stateNotification.getEvent().getExternalId(),
                        stateNotification.getState().getId(),
                        stateNotification.getCurrentValue()));
    }
}