package com.wearablescloud.apidocgen.model;

import java.util.List;

public record ParsedSpec(
        String title,
        String version,
        String description,
        List<String> servers,
        List<ParsedEndpoint> endpoints,
        List<SecurityScheme> securitySchemes
) {
}
