package com.wearablescloud.apidocgen.service;

import com.wearablescloud.apidocgen.exception.SpecNotFoundException;
import com.wearablescloud.apidocgen.model.GeneratedEndpointDoc;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateless-per-process, in-memory store keyed by a UUID handed back on upload.
 * Deliberately not a database: this tool is a single-user local dev utility, and
 * the spec/generated docs only need to survive across the handful of REST calls
 * that make up one browsing session.
 */
@Component
public class SpecRegistry {

    private final Map<UUID, ParsedSpec> specs = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, GeneratedEndpointDoc>> generatedDocs = new ConcurrentHashMap<>();
    private final Map<UUID, String> authExplanations = new ConcurrentHashMap<>();

    public UUID store(ParsedSpec spec) {
        UUID id = UUID.randomUUID();
        specs.put(id, spec);
        return id;
    }

    public ParsedSpec get(UUID specId) {
        ParsedSpec spec = specs.get(specId);
        if (spec == null) {
            throw new SpecNotFoundException("No spec found for id " + specId);
        }
        return spec;
    }

    public void putGeneratedDoc(UUID specId, GeneratedEndpointDoc doc) {
        get(specId); // validates existence
        generatedDocs.computeIfAbsent(specId, k -> new ConcurrentHashMap<>()).put(doc.operationId(), doc);
    }

    public Map<String, GeneratedEndpointDoc> getGeneratedDocs(UUID specId) {
        get(specId); // validates existence
        return generatedDocs.getOrDefault(specId, Map.of());
    }

    public void putAuthExplanation(UUID specId, String explanation) {
        get(specId); // validates existence
        authExplanations.put(specId, explanation);
    }

    public String getAuthExplanationOrNull(UUID specId) {
        get(specId); // validates existence
        return authExplanations.get(specId);
    }
}
