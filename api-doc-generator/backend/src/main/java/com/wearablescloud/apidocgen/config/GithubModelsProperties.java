package com.wearablescloud.apidocgen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.github-models")
public record GithubModelsProperties(String token, String baseUrl, String model) {
}
