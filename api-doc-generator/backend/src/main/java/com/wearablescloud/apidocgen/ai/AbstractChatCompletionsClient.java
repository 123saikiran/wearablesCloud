package com.wearablescloud.apidocgen.ai;

import com.wearablescloud.apidocgen.ai.dto.ChatCompletionRequest;
import com.wearablescloud.apidocgen.ai.dto.ChatCompletionResponse;
import com.wearablescloud.apidocgen.ai.dto.ChatMessage;
import com.wearablescloud.apidocgen.exception.AiProviderException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Shared HTTP call logic for chat-completions-compatible providers (OpenAI and
 * GitHub Models both speak this shape). Subclasses just supply endpoint/credential
 * configuration.
 */
abstract class AbstractChatCompletionsClient implements AiDocGenerationClient {

    private static final String SYSTEM_PROMPT =
            "You are an expert technical writer and Java Spring Boot developer generating API documentation.";

    protected final RestClient restClient;
    protected final String model;
    protected final String providerName;

    protected AbstractChatCompletionsClient(RestClient.Builder builder, String baseUrl,
                                             String apiKeyOrToken, String model, String providerName) {
        this.model = model;
        this.providerName = providerName;
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + (apiKeyOrToken == null ? "" : apiKeyOrToken))
                .build();
        if (apiKeyOrToken == null || apiKeyOrToken.isBlank()) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("No credential configured for AI provider '{}' - calls will fail until one is set", providerName);
        }
    }

    @Override
    public String generateEndpointDoc(String prompt) {
        return complete(prompt);
    }

    @Override
    public String generateAuthExplanation(String prompt) {
        return complete(prompt);
    }

    private String complete(String userPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(ChatMessage.system(SYSTEM_PROMPT), ChatMessage.user(userPrompt)),
                0.3
        );
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiProviderException(providerName + " returned no completion choices");
            }
            return response.choices().get(0).message().content();
        } catch (RestClientException e) {
            throw new AiProviderException(providerName + " call failed: " + e.getMessage(), e);
        }
    }
}
