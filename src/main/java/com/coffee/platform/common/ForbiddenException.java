package com.coffee.platform.common;

public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(403, "FORBIDDEN", message);
    }
}
