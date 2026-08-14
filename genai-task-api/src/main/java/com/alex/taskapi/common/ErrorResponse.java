package com.alex.taskapi.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorResponse> fieldErrors) {

    public ErrorResponse(int status, String error, String message, Instant timestamp) {
        this(status, error, message, timestamp, List.of());
    }
}
