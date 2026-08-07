package com.wearablescloud.apidocgen.ai;

import com.wearablescloud.apidocgen.config.GithubModelsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "ai", name = "provider", havingValue = "github-models")
public class GithubModelsDocGenerationClient extends AbstractChatCompletionsClient {

    public GithubModelsDocGenerationClient(RestClient.Builder aiRestClientBuilder, GithubModelsProperties properties) {
        super(aiRestClientBuilder, properties.baseUrl(), properties.token(), properties.model(), "GitHub Models");
    }
}
