package com.coffee.platform.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        String traceId,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}
}
