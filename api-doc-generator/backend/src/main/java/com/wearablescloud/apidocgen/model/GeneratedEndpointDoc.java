package com.wearablescloud.apidocgen.model;

public record GeneratedEndpointDoc(
        String operationId,
        String humanDoc,
        String exampleRequest,
        String exampleResponse,
        String javaClientSnippet,
        String errorExplanations
) {
}
