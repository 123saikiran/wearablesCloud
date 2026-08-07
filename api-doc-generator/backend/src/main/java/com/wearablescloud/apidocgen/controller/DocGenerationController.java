package com.wearablescloud.apidocgen.controller;

import com.wearablescloud.apidocgen.dto.AuthExplanationResponse;
import com.wearablescloud.apidocgen.model.GeneratedEndpointDoc;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import com.wearablescloud.apidocgen.service.DocGenerationService;
import com.wearablescloud.apidocgen.service.SpecRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/specs/{specId}")
public class DocGenerationController {

    private final DocGenerationService docGenerationService;
    private final SpecRegistry specRegistry;

    public DocGenerationController(DocGenerationService docGenerationService, SpecRegistry specRegistry) {
        this.docGenerationService = docGenerationService;
        this.specRegistry = specRegistry;
    }

    @PostMapping("/endpoints/{operationId}/generate")
    public GeneratedEndpointDoc generateForEndpoint(@PathVariable UUID specId, @PathVariable String operationId) {
        return docGenerationService.generateForEndpoint(specId, operationId);
    }

    @PostMapping("/generate-all")
    public Map<String, GeneratedEndpointDoc> generateAll(@PathVariable UUID specId) {
        return docGenerationService.generateForAllEndpoints(specId);
    }

    @GetMapping("/auth-explanation")
    public AuthExplanationResponse authExplanation(@PathVariable UUID specId) {
        ParsedSpec spec = specRegistry.get(specId);
        String explanation = docGenerationService.explainAuth(specId);
        return new AuthExplanationResponse(spec.securitySchemes(), explanation);
    }
}
