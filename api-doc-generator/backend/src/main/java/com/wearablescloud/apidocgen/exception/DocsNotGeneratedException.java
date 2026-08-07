package com.wearablescloud.apidocgen.exception;

public class DocsNotGeneratedException extends RuntimeException {

    public DocsNotGeneratedException(String message) {
        super(message);
    }
}
