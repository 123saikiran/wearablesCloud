package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wearablescloud.apidocgen.exception.SpecParseException;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedParameter;
import com.wearablescloud.apidocgen.model.ParsedSchema;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import com.wearablescloud.apidocgen.model.SecurityScheme;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses an OpenAPI 3.x (best-effort Swagger 2.0) JSON spec into the app's own
 * simplified domain model using plain Jackson tree-walking - no swagger-parser
 * dependency. {@code $ref} resolution is limited to {@code components.schemas}
 * one level of indirection, guarded against cycles.
 */
@Service
public class OpenApiParserService {

    private static final List<String> HTTP_METHODS =
            List.of("get", "put", "post", "delete", "options", "head", "patch", "trace");

    private final ObjectMapper objectMapper;

    public OpenApiParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedSpec parse(InputStream specInputStream) {
        JsonNode root;
        try {
            root = objectMapper.readTree(specInputStream);
        } catch (IOException e) {
            throw new SpecParseException("Uploaded file is not valid JSON: " + e.getMessage(), e);
        }
        if (root == null || root.isMissingNode() || !root.isObject()) {
            throw new SpecParseException("Uploaded file does not contain a JSON object");
        }

        JsonNode info = root.path("info");
        String title = info.path("title").asText("Untitled API");
        String version = info.path("version").asText("unknown");
        String description = info.path("description").asText(null);

        List<String> servers = new ArrayList<>();
        for (JsonNode server : root.path("servers")) {
            String url = server.path("url").asText(null);
            if (url != null) {
                servers.add(url);
            }
        }

        Map<String, ParsedSchema> schemaComponents = parseSchemaComponents(root);
        List<SecurityScheme> securitySchemes = parseSecuritySchemes(root);
        List<ParsedEndpoint> endpoints = parseEndpoints(root, schemaComponents);

        if (endpoints.isEmpty()) {
            throw new SpecParseException("No operations found under 'paths' in the uploaded spec");
        }

        return new ParsedSpec(title, version, description, servers, endpoints, securitySchemes);
    }

    private Map<String, ParsedSchema> parseSchemaComponents(JsonNode root) {
        JsonNode schemasNode = root.path("components").path("schemas");
        Map<String, ParsedSchema> resolved = new LinkedHashMap<>();
        if (!schemasNode.isObject()) {
            return resolved;
        }
        var fieldNames = schemasNode.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            resolved.put(name, null); // reserve the key so cyclic refs stop instead of recursing forever
        }
        fieldNames = schemasNode.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            resolved.put(name, toSchema(schemasNode.get(name), schemasNode, Set.of(name)));
        }
        return resolved;
    }

    private ParsedSchema toSchema(JsonNode node, JsonNode schemasNode, Set<String> visitedRefs) {
        if (node == null || node.isMissingNode() || !node.isObject()) {
            return null;
        }

        String ref = node.path("$ref").asText(null);
        if (ref != null) {
            String refName = refName(ref);
            if (refName == null || visitedRefs.contains(refName)) {
                return new ParsedSchema("object", null, "circular or unresolved reference to " + ref,
                        Map.of(), null, List.of(), null);
            }
            JsonNode target = schemasNode.get(refName);
            Set<String> nextVisited = new java.util.HashSet<>(visitedRefs);
            nextVisited.add(refName);
            return toSchema(target, schemasNode, nextVisited);
        }

        String type = node.path("type").asText(null);
        String format = node.path("format").asText(null);
        String description = node.path("description").asText(null);
        String example = node.has("example") ? node.get("example").toString() : null;

        Map<String, ParsedSchema> properties = new LinkedHashMap<>();
        JsonNode propsNode = node.path("properties");
        if (propsNode.isObject()) {
            var propNames = propsNode.fieldNames();
            while (propNames.hasNext()) {
                String propName = propNames.next();
                ParsedSchema propSchema = toSchema(propsNode.get(propName), schemasNode, visitedRefs);
                if (propSchema != null) {
                    properties.put(propName, propSchema);
                }
            }
        }

        ParsedSchema items = null;
        if (node.has("items")) {
            items = toSchema(node.get("items"), schemasNode, visitedRefs);
        }

        List<String> required = new ArrayList<>();
        for (JsonNode req : node.path("required")) {
            required.add(req.asText());
        }

        if (type == null && !properties.isEmpty()) {
            type = "object";
        }

        return new ParsedSchema(type, format, description, properties, items, required, example);
    }

    private String refName(String ref) {
        String marker = "#/components/schemas/";
        if (ref.startsWith(marker)) {
            return ref.substring(marker.length());
        }
        return null;
    }

    private List<SecurityScheme> parseSecuritySchemes(JsonNode root) {
        List<SecurityScheme> result = new ArrayList<>();
        JsonNode schemesNode = root.path("components").path("securitySchemes");
        if (!schemesNode.isObject()) {
            schemesNode = root.path("securityDefinitions"); // Swagger 2.0 fallback
        }
        if (!schemesNode.isObject()) {
            return result;
        }
        var fieldNames = schemesNode.fieldNames();
        while (fieldNames.hasNext()) {
            String name = fieldNames.next();
            JsonNode scheme = schemesNode.get(name);
            result.add(new SecurityScheme(
                    name,
                    scheme.path("type").asText(null),
                    scheme.path("scheme").asText(null),
                    scheme.path("bearerFormat").asText(null),
                    scheme.path("in").asText(null),
                    scheme.path("description").asText(null)
            ));
        }
        return result;
    }

    private List<ParsedEndpoint> parseEndpoints(JsonNode root, Map<String, ParsedSchema> schemaComponents) {
        List<ParsedEndpoint> endpoints = new ArrayList<>();
        JsonNode schemasNode = root.path("components").path("schemas");
        JsonNode pathsNode = root.path("paths");
        if (!pathsNode.isObject()) {
            return endpoints;
        }

        var pathNames = pathsNode.fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            JsonNode pathItem = pathsNode.get(path);

            List<ParsedParameter> pathLevelParams = parseParameters(pathItem.path("parameters"), schemasNode);

            for (String method : HTTP_METHODS) {
                if (!pathItem.has(method)) {
                    continue;
                }
                JsonNode operation = pathItem.get(method);

                List<ParsedParameter> params = new ArrayList<>(pathLevelParams);
                params.addAll(parseParameters(operation.path("parameters"), schemasNode));

                ParsedSchema requestBodySchema = parseRequestBody(operation.path("requestBody"), schemasNode);
                Map<String, ParsedSchema> responses = parseResponses(operation.path("responses"), schemasNode);
                List<String> securitySchemeNames = parseOperationSecurity(operation);

                String operationId = operation.path("operationId").asText(null);
                if (operationId == null || operationId.isBlank()) {
                    operationId = method.toUpperCase() + "_" + path.replaceAll("[{}/]", "_").replaceAll("_+", "_");
                }

                endpoints.add(new ParsedEndpoint(
                        path,
                        method.toUpperCase(),
                        operationId,
                        operation.path("summary").asText(null),
                        operation.path("description").asText(null),
                        params,
                        requestBodySchema,
                        responses,
                        securitySchemeNames
                ));
            }
        }
        return endpoints;
    }

    private List<ParsedParameter> parseParameters(JsonNode parametersNode, JsonNode schemasNode) {
        List<ParsedParameter> result = new ArrayList<>();
        if (!parametersNode.isArray()) {
            return result;
        }
        for (JsonNode param : parametersNode) {
            result.add(new ParsedParameter(
                    param.path("name").asText(null),
                    param.path("in").asText(null),
                    param.path("required").asBoolean(false),
                    param.path("description").asText(null),
                    toSchema(param.path("schema"), schemasNode, Set.of())
            ));
        }
        return result;
    }

    private ParsedSchema parseRequestBody(JsonNode requestBodyNode, JsonNode schemasNode) {
        if (requestBodyNode == null || requestBodyNode.isMissingNode()) {
            return null;
        }
        JsonNode jsonContent = requestBodyNode.path("content").path("application/json").path("schema");
        if (jsonContent.isMissingNode()) {
            return null;
        }
        return toSchema(jsonContent, schemasNode, Set.of());
    }

    private Map<String, ParsedSchema> parseResponses(JsonNode responsesNode, JsonNode schemasNode) {
        Map<String, ParsedSchema> result = new LinkedHashMap<>();
        if (!responsesNode.isObject()) {
            return result;
        }
        var statusCodes = responsesNode.fieldNames();
        while (statusCodes.hasNext()) {
            String statusCode = statusCodes.next();
            JsonNode responseNode = responsesNode.get(statusCode);
            JsonNode jsonSchema = responseNode.path("content").path("application/json").path("schema");
            result.put(statusCode, jsonSchema.isMissingNode() ? null : toSchema(jsonSchema, schemasNode, Set.of()));
        }
        return result;
    }

    private List<String> parseOperationSecurity(JsonNode operation) {
        List<String> names = new ArrayList<>();
        for (JsonNode requirement : operation.path("security")) {
            var fieldNames = requirement.fieldNames();
            while (fieldNames.hasNext()) {
                names.add(fieldNames.next());
            }
        }
        return names;
    }
}
