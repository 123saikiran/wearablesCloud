package com.wearablescloud.apidocgen.model;

public record SecurityScheme(
        String name,
        String type,
        String scheme,
        String bearerFormat,
        String in,
        String description
) {
}
