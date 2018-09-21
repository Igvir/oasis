package io.github.isuru.oasis.services.api.routers;

import io.github.isuru.oasis.model.Constants;
import io.github.isuru.oasis.services.api.IEventsService;
import io.github.isuru.oasis.services.api.IOasisApiService;
import io.github.isuru.oasis.services.api.dto.EventPushDto;
import io.github.isuru.oasis.services.api.dto.EventSourceDto;
import io.github.isuru.oasis.services.exception.ApiAuthException;
import io.github.isuru.oasis.services.exception.InputValidationException;
import io.github.isuru.oasis.services.utils.AuthUtils;
import io.github.isuru.oasis.services.utils.Checks;
import io.github.isuru.oasis.services.utils.EventSourceToken;
import io.github.isuru.oasis.services.utils.UserRole;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author iweerarathna
 */
public class EventsRouter extends BaseRouters {

    EventsRouter(IOasisApiService apiService) {
        super(apiService);
    }

    @Override
    public void register() {
        post("/submit", this::submitEvent);

        Spark.path("/source", () -> {
            post("/add", this::addEventSource, UserRole.ADMIN);
            post("/list", this::listEventSources, UserRole.ADMIN);
            post("/:sourceId/downloadkey", this::downloadKey, UserRole.ADMIN);
            delete("/:sourceId", this::disableEventSource, UserRole.ADMIN);
        });
    }

    private Object submitEvent(Request req, Response res) throws Exception {
        IEventsService es = getApiService().getEventService();
        String token = extractToken(req);

        // verify event submission
        String body = req.body();
        String[] ids = token.split("[:]");
        if (ids.length != 2) {
            throw new ApiAuthException("Invalid format of event authorization header!");
        }
        Optional<EventSourceToken> appIdOpt = es.readSourceByToken(ids[0]);
        EventSourceToken sourceToken = appIdOpt.orElseThrow((Supplier<Exception>)
                () -> new ApiAuthException("Event source token is not recognized by the Oasis!"));
        AuthUtils.verifyIntegrity(sourceToken, ids[1], body);

        EventPushDto eventPushDto = bodyAs(req, EventPushDto.class);
        Map<String, Object> meta = eventPushDto.getMeta() == null ? new HashMap<>() : eventPushDto.getMeta();
        Long gid = meta.containsKey("gameId") ? null : Long.parseLong(meta.get("gameId").toString());

        if (eventPushDto.getEvent() != null) {
            Map<String, Object> event = eventPushDto.getEvent();
            event.put(Constants.FIELD_GAME_ID, gid);
            event.putAll(meta);
            es.submitEvent(token, event);
        } else if (eventPushDto.getEvents() != null) {
            eventPushDto.getEvents().forEach(et -> {
                et.put(Constants.FIELD_GAME_ID, gid);
                et.putAll(meta);
            });
            es.submitEvents(token, eventPushDto.getEvents());
        } else {
            throw new InputValidationException("No events have been defined in this call!");
        }
        return asResBool(true);
    }

    private Object downloadKey(Request req, Response res) throws Exception {
        Integer sourceId = asPInt(req, "sourceId");
        Checks.greaterThanZero(sourceId, "sourceId");

        Optional<EventSourceToken> optionalToken = getApiService().getEventService().makeDownloadSourceKey(sourceId);
        if (optionalToken.isPresent()) {
            EventSourceToken eventSourceToken = optionalToken.get();
            String sourceName = String.format("key-%s", eventSourceToken.getSourceName());

            res.header("Content-Disposition", String.format("attachment; filename=key-%s.key", sourceName));
            res.header("Access-Control-Expose-Headers", "Content-Disposition");

            res.raw().setContentType("application/octet-stream");
            OutputStream outputStream = res.raw().getOutputStream();
            IOUtils.copy(eventSourceToken.getSecretKey().getBinaryStream(), outputStream);
            outputStream.flush();
            res.status(200);
            return res;
        } else {
            throw new InputValidationException("You are not allowed to download key twice! " +
                    "Contact admin if you lost the key.");
        }
    }

    private Object disableEventSource(Request req, Response res) throws Exception {
        return getApiService().getEventService().disableEventSource(asPInt(req, "sourceId"));
    }

    private Object listEventSources(Request req, Response res) throws Exception {
        return getApiService().getEventService().listAllEventSources()
                .stream()
                .map(EventSourceDto::from)
                .collect(Collectors.toList());
    }

    private Object addEventSource(Request req, Response res) throws Exception {
        EventSourceToken token = bodyAs(req, EventSourceToken.class);
        IEventsService eventService = getApiService().getEventService();

        // duplicate events source names are ignored.
        if (eventService.listAllEventSources().stream()
                .anyMatch(e -> e.getDisplayName().equalsIgnoreCase(token.getDisplayName()))) {
            throw new InputValidationException(
                    "There is already an event token exist with name '" + token.getDisplayName() + "'!");
        }

        return eventService.addEventSource(token);
    }

    private String extractToken(Request request) throws ApiAuthException {
        String auth = request.headers(AUTHORIZATION);
        if (auth != null) {
            if (auth.startsWith(BEARER)) {
                return auth.substring(BEARER.length());
            }
        }
        throw new ApiAuthException("The token is not found with the event.");
    }
}
