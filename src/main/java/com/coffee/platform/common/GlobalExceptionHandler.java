package com.coffee.platform.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex, HttpServletRequest req) {
        String traceId = (String) req.getAttribute(TraceIdFilter.TRACE_ID);
        var body = new ErrorResponse(ex.getCode(), ex.getMessage(), traceId, null);
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        String traceId = (String) req.getAttribute(TraceIdFilter.TRACE_ID);
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        var body = new ErrorResponse("VALIDATION_ERROR", "Validation failed", traceId, fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }
}
