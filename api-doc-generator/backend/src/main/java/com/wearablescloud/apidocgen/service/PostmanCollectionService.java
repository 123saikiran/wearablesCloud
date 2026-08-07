package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedParameter;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import com.wearablescloud.apidocgen.model.SecurityScheme;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds a real, importable Postman Collection v2.1 JSON mechanically from the
 * parsed spec - no AI call needed, since this is a deterministic transformation.
 */
@Service
public class PostmanCollectionService {

    private static final String SCHEMA_URL =
            "https://schema.getpostman.com/json/collection/v2.1.0/collection.json";

    private final ObjectMapper objectMapper;

    public PostmanCollectionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PostmanCollectionResult build(ParsedSpec spec) {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode info = root.putObject("info");
        info.put("name", spec.title());
        info.put("description", spec.description() == null ? "" : spec.description());
        info.put("schema", SCHEMA_URL);

        String baseUrl = spec.servers() != null && !spec.servers().isEmpty() ? spec.servers().get(0) : "{{baseUrl}}";

        ArrayNode items = root.putArray("item");
        for (ParsedEndpoint endpoint : spec.endpoints()) {
            items.add(buildItem(endpoint, baseUrl));
        }

        String summary = "%d requests across %d endpoint(s), using %s"
                .formatted(spec.endpoints().size(), spec.endpoints().size(), authSummary(spec.securitySchemes()));

        return new PostmanCollectionResult(root, summary);
    }

    private ObjectNode buildItem(ParsedEndpoint endpoint, String baseUrl) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("name", endpoint.summary() != null ? endpoint.summary() : endpoint.operationId());

        ObjectNode request = item.putObject("request");
        request.put("method", endpoint.method());

        ArrayNode headers = request.putArray("header");
        for (String schemeName : endpoint.securitySchemeNames()) {
            ObjectNode header = objectMapper.createObjectNode();
            header.put("key", "Authorization");
            header.put("value", "Bearer {{" + schemeName + "Token}}");
            headers.add(header);
        }

        ObjectNode url = request.putObject("url");
        String rawUrl = baseUrl.replaceAll("/$", "") + endpoint.path();
        url.put("raw", rawUrl);

        ArrayNode queryParams = url.putArray("query");
        for (ParsedParameter param : endpoint.parameters()) {
            if ("query".equals(param.in())) {
                ObjectNode queryParam = objectMapper.createObjectNode();
                queryParam.put("key", param.name());
                queryParam.put("value", "");
                queryParams.add(queryParam);
            }
        }

        if (endpoint.requestBodySchema() != null) {
            ObjectNode body = request.putObject("body");
            body.put("mode", "raw");
            body.put("raw", "{}");
            ObjectNode options = body.putObject("options");
            ObjectNode raw = options.putObject("raw");
            raw.put("language", "json");
        }

        return item;
    }

    private String authSummary(java.util.List<SecurityScheme> schemes) {
        if (schemes == null || schemes.isEmpty()) {
            return "no authentication";
        }
        Set<String> types = new LinkedHashSet<>();
        for (SecurityScheme scheme : schemes) {
            types.add(scheme.type() != null ? scheme.type() : "unknown");
        }
        return String.join(" + ", types) + " auth";
    }
}
