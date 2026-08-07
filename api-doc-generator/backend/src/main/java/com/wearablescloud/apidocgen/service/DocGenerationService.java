package com.wearablescloud.apidocgen.service;

import com.wearablescloud.apidocgen.ai.AiDocGenerationClient;
import com.wearablescloud.apidocgen.exception.SpecParseException;
import com.wearablescloud.apidocgen.model.GeneratedEndpointDoc;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocGenerationService {

    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "===(DESCRIPTION|EXAMPLE_REQUEST|EXAMPLE_RESPONSE|JAVA_CLIENT_SNIPPET|ERROR_EXPLANATIONS)===");

    private final SpecRegistry specRegistry;
    private final PromptBuilderService promptBuilderService;
    private final AiDocGenerationClient aiDocGenerationClient;

    public DocGenerationService(SpecRegistry specRegistry,
                                 PromptBuilderService promptBuilderService,
                                 AiDocGenerationClient aiDocGenerationClient) {
        this.specRegistry = specRegistry;
        this.promptBuilderService = promptBuilderService;
        this.aiDocGenerationClient = aiDocGenerationClient;
    }

    public GeneratedEndpointDoc generateForEndpoint(UUID specId, String operationId) {
        ParsedSpec spec = specRegistry.get(specId);
        ParsedEndpoint endpoint = findEndpoint(spec, operationId);

        String prompt = promptBuilderService.buildEndpointPrompt(endpoint);
        String rawResponse = aiDocGenerationClient.generateEndpointDoc(prompt);
        GeneratedEndpointDoc doc = splitIntoSections(operationId, rawResponse);

        specRegistry.putGeneratedDoc(specId, doc);
        return doc;
    }

    public Map<String, GeneratedEndpointDoc> generateForAllEndpoints(UUID specId) {
        ParsedSpec spec = specRegistry.get(specId);
        Map<String, GeneratedEndpointDoc> docs = new LinkedHashMap<>();
        for (ParsedEndpoint endpoint : spec.endpoints()) {
            docs.put(endpoint.operationId(), generateForEndpoint(specId, endpoint.operationId()));
        }
        explainAuth(specId);
        return docs;
    }

    public String explainAuth(UUID specId) {
        ParsedSpec spec = specRegistry.get(specId);
        String prompt = promptBuilderService.buildAuthExplanationPrompt(spec.securitySchemes());
        String explanation = aiDocGenerationClient.generateAuthExplanation(prompt);
        specRegistry.putAuthExplanation(specId, explanation);
        return explanation;
    }

    private ParsedEndpoint findEndpoint(ParsedSpec spec, String operationId) {
        return spec.endpoints().stream()
                .filter(e -> e.operationId().equals(operationId))
                .findFirst()
                .orElseThrow(() -> new SpecParseException("No endpoint with operationId '" + operationId + "'"));
    }

    private GeneratedEndpointDoc splitIntoSections(String operationId, String rawResponse) {
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher matcher = SECTION_PATTERN.matcher(rawResponse);

        int previousEnd = -1;
        String previousMarker = null;
        while (matcher.find()) {
            if (previousMarker != null) {
                sections.put(previousMarker, rawResponse.substring(previousEnd, matcher.start()).trim());
            }
            previousMarker = matcher.group(1);
            previousEnd = matcher.end();
        }
        if (previousMarker != null) {
            sections.put(previousMarker, rawResponse.substring(previousEnd).trim());
        }

        return new GeneratedEndpointDoc(
                operationId,
                sections.getOrDefault("DESCRIPTION", rawResponse.trim()),
                sections.getOrDefault("EXAMPLE_REQUEST", ""),
                sections.getOrDefault("EXAMPLE_RESPONSE", ""),
                sections.getOrDefault("JAVA_CLIENT_SNIPPET", ""),
                sections.getOrDefault("ERROR_EXPLANATIONS", "")
        );
    }
}
