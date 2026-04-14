package com.sqlforge.backend.web;

import com.sqlforge.backend.model.ConnectionDefinition;
import com.sqlforge.backend.model.ConnectionProbeResult;
import com.sqlforge.backend.model.ConnectionValidationResult;
import com.sqlforge.backend.service.ConnectionProbeService;
import com.sqlforge.backend.service.ConnectionService;
import com.sqlforge.backend.web.dto.ConnectionRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final ConnectionProbeService connectionProbeService;

    public ConnectionController(
        ConnectionService connectionService,
        ConnectionProbeService connectionProbeService
    ) {
        this.connectionService = connectionService;
        this.connectionProbeService = connectionProbeService;
    }

    @GetMapping
    public Map<String, Object> listConnections() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("connections", connectionService.listConnections());
        return payload;
    }

    @PostMapping("/validate")
    public Map<String, Object> validate(@Valid @RequestBody ConnectionRequest request) {
        ConnectionValidationResult validationResult = connectionService.validate(request);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("validation", validationResult);
        return payload;
    }

    @PostMapping("/probe")
    public Map<String, Object> probe(@Valid @RequestBody ConnectionRequest request) {
        ConnectionValidationResult validationResult = connectionService.validate(request);

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(String.join("; ", validationResult.getMessages()));
        }

        ConnectionProbeResult probeResult = connectionProbeService.probe(request);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("probe", probeResult);
        return payload;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody ConnectionRequest request) {
        ConnectionDefinition connectionDefinition = connectionService.create(request);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("connection", connectionDefinition);
        return payload;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException exception) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("message", exception.getMessage());
        return payload;
    }
}
