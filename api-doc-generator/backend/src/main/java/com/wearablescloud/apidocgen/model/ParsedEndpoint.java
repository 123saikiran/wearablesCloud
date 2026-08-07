package com.wearablescloud.apidocgen.model;

import java.util.List;
import java.util.Map;

public record ParsedEndpoint(
        String path,
        String method,
        String operationId,
        String summary,
        String description,
        List<ParsedParameter> parameters,
        ParsedSchema requestBodySchema,
        Map<String, ParsedSchema> responsesByStatusCode,
        List<String> securitySchemeNames
) {
}
