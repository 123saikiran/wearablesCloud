package com.wearablescloud.apidocgen.model;

public record ParsedParameter(
        String name,
        String in,
        boolean required,
        String description,
        ParsedSchema schema
) {
}
