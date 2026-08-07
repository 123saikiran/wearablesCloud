package com.wearablescloud.apidocgen.controller;

import com.wearablescloud.apidocgen.dto.EndpointSummaryResponse;
import com.wearablescloud.apidocgen.dto.UploadSpecResponse;
import com.wearablescloud.apidocgen.exception.SpecParseException;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import com.wearablescloud.apidocgen.model.SecurityScheme;
import com.wearablescloud.apidocgen.service.OpenApiParserService;
import com.wearablescloud.apidocgen.service.SpecRegistry;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/specs")
public class SpecController {

    private final OpenApiParserService parserService;
    private final SpecRegistry specRegistry;

    public SpecController(OpenApiParserService parserService, SpecRegistry specRegistry) {
        this.parserService = parserService;
        this.specRegistry = specRegistry;
    }

    @PostMapping
    public UploadSpecResponse uploadSpec(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new SpecParseException("Uploaded file is empty");
        }
        ParsedSpec spec;
        try {
            spec = parserService.parse(file.getInputStream());
        } catch (IOException e) {
            throw new SpecParseException("Could not read uploaded file: " + e.getMessage(), e);
        }
        UUID specId = specRegistry.store(spec);
        List<String> securitySchemeNames = spec.securitySchemes().stream().map(SecurityScheme::name).toList();
        return new UploadSpecResponse(specId, spec.title(), spec.version(), spec.endpoints().size(), securitySchemeNames);
    }

    @GetMapping("/{specId}/endpoints")
    public List<EndpointSummaryResponse> listEndpoints(@PathVariable UUID specId) {
        ParsedSpec spec = specRegistry.get(specId);
        return spec.endpoints().stream()
                .map(this::toSummary)
                .toList();
    }

    private EndpointSummaryResponse toSummary(ParsedEndpoint endpoint) {
        return new EndpointSummaryResponse(endpoint.operationId(), endpoint.method(), endpoint.path(), endpoint.summary());
    }
}
