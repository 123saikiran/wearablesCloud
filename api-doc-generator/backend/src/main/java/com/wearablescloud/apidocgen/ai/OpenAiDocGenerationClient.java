package com.wearablescloud.apidocgen.ai;

import com.wearablescloud.apidocgen.config.OpenAiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiDocGenerationClient extends AbstractChatCompletionsClient {

    public OpenAiDocGenerationClient(RestClient.Builder aiRestClientBuilder, OpenAiProperties properties) {
        super(aiRestClientBuilder, properties.baseUrl(), properties.apiKey(), properties.model(), "OpenAI");
    }
}
