package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wearablescloud.apidocgen.exception.SpecParseException;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedParameter;
import com.wearablescloud.apidocgen.model.SecurityScheme;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PromptBuilderService {

    private final ObjectMapper objectMapper;
    private final String endpointDocTemplate;
    private final String authExplanationTemplate;

    public PromptBuilderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.endpointDocTemplate = readTemplate("prompts/endpoint-doc.st");
        this.authExplanationTemplate = readTemplate("prompts/auth-explanation.st");
    }

    public String buildEndpointPrompt(ParsedEndpoint endpoint) {
        return endpointDocTemplate
                .replace("{{method}}", nullToEmpty(endpoint.method()))
                .replace("{{path}}", nullToEmpty(endpoint.path()))
                .replace("{{operationId}}", nullToEmpty(endpoint.operationId()))
                .replace("{{summary}}", nullToEmpty(endpoint.summary()))
                .replace("{{description}}", nullToEmpty(endpoint.description()))
                .replace("{{parametersBlock}}", parametersBlock(endpoint.parameters()))
                .replace("{{requestBodySchemaJson}}", toJson(endpoint.requestBodySchema()))
                .replace("{{responsesJson}}", toJson(endpoint.responsesByStatusCode()))
                .replace("{{securitySchemeNames}}", String.join(", ", endpoint.securitySchemeNames()));
    }

    public String buildAuthExplanationPrompt(List<SecurityScheme> securitySchemes) {
        return authExplanationTemplate.replace("{{securitySchemesJson}}", toJson(securitySchemes));
    }

    private String parametersBlock(List<ParsedParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (ParsedParameter p : parameters) {
            sb.append("- ").append(p.name())
                    .append(" (").append(p.in()).append(", ")
                    .append(p.required() ? "required" : "optional").append(") ")
                    .append(p.schema() != null ? p.schema().type() : "unknown type");
            if (p.description() != null) {
                sb.append(" - ").append(p.description());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String readTemplate(String classpathLocation) {
        try {
            byte[] bytes = new ClassPathResource(classpathLocation).getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SpecParseException("Failed to load prompt template " + classpathLocation, e);
        }
    }
}
