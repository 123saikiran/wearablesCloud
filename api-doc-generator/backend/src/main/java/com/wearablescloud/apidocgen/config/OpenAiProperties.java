package com.wearablescloud.apidocgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.openai")
public record OpenAiProperties(String apiKey, String baseUrl, String model) {
}
