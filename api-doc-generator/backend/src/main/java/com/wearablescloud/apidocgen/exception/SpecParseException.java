package com.wearablescloud.apidocgen.exception;

public class SpecParseException extends RuntimeException {

    public SpecParseException(String message) {
        super(message);
    }

    public SpecParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
