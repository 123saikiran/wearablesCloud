package com.wearablescloud.apidocgen.service;

import com.wearablescloud.apidocgen.model.GeneratedEndpointDoc;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MarkdownRenderService {

    public String render(ParsedSpec spec, Map<String, GeneratedEndpointDoc> docsByOperationId, String authExplanation) {
        StringBuilder md = new StringBuilder();

        md.append("# ").append(spec.title()).append('\n');
        if (spec.description() != null) {
            md.append('\n').append(spec.description()).append('\n');
        }
        md.append("\nVersion: ").append(spec.version()).append("\n\n");

        md.append("## Table of Contents\n\n");
        for (ParsedEndpoint endpoint : spec.endpoints()) {
            md.append("- [").append(endpoint.method()).append(' ').append(endpoint.path()).append("](#")
                    .append(anchor(endpoint.operationId())).append(")\n");
        }
        md.append("- [Authentication](#authentication)\n\n");

        for (ParsedEndpoint endpoint : spec.endpoints()) {
            GeneratedEndpointDoc doc = docsByOperationId.get(endpoint.operationId());
            md.append("## ").append(endpoint.method()).append(' ').append(endpoint.path())
                    .append(" {#").append(anchor(endpoint.operationId())).append("}\n\n");

            if (doc == null) {
                md.append("_Documentation not generated for this endpoint._\n\n");
                continue;
            }

            md.append("### Description\n\n").append(doc.humanDoc()).append("\n\n");
            md.append("### Example Request\n\n").append(doc.exampleRequest()).append("\n\n");
            md.append("### Example Response\n\n").append(doc.exampleResponse()).append("\n\n");
            md.append("### Java Spring Boot Client\n\n").append(doc.javaClientSnippet()).append("\n\n");
            md.append("### Errors\n\n").append(doc.errorExplanations()).append("\n\n");
        }

        md.append("## Authentication {#authentication}\n\n");
        md.append(authExplanation != null ? authExplanation : "_No authentication explanation generated._");
        md.append('\n');

        return md.toString();
    }

    private String anchor(String operationId) {
        return operationId.toLowerCase().replaceAll("[^a-z0-9]+", "-");
    }
}
