package com.wearablescloud.apidocgen.ai.dto;

import java.util.List;

/**
 * Request shape shared by OpenAI's Chat Completions API and GitHub Models'
 * OpenAI-compatible inference endpoint - both accept identical JSON here, so
 * one DTO serves both {@link com.wearablescloud.apidocgen.ai.OpenAiDocGenerationClient}
 * and {@link com.wearablescloud.apidocgen.ai.GithubModelsDocGenerationClient}.
 */
public record ChatCompletionRequest(String model, List<ChatMessage> messages, double temperature) {
}
