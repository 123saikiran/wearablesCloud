package com.wearablescloud.apidocgen.controller;

import com.wearablescloud.apidocgen.config.AiProviderProperties;
import com.wearablescloud.apidocgen.dto.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final AiProviderProperties aiProviderProperties;

    public HealthController(AiProviderProperties aiProviderProperties) {
        this.aiProviderProperties = aiProviderProperties;
    }

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("ok", aiProviderProperties.provider());
    }
}
