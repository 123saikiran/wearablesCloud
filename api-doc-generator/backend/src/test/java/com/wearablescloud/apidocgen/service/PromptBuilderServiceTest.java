package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedParameter;
import com.wearablescloud.apidocgen.model.ParsedSchema;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderServiceTest {

    private final PromptBuilderService promptBuilderService = new PromptBuilderService(new ObjectMapper());

    @Test
    void buildsEndpointPromptWithSubstitutedPlaceholders() {
        ParsedParameter petIdParam = new ParsedParameter("petId", "path", true, "ID of the pet",
                new ParsedSchema("integer", "int64", null, Map.of(), null, List.of(), null));

        ParsedEndpoint endpoint = new ParsedEndpoint(
                "/pets/{petId}", "GET", "getPetById", "Get a pet by ID", "Returns a single pet.",
                List.of(petIdParam), null, Collections.singletonMap("200", null), List.of("api_key"));

        String prompt = promptBuilderService.buildEndpointPrompt(endpoint);

        assertThat(prompt).contains("Method: GET");
        assertThat(prompt).contains("Path: /pets/{petId}");
        assertThat(prompt).contains("Operation ID: getPetById");
        assertThat(prompt).contains("petId (path, required) integer");
        assertThat(prompt).contains("Security requirements: api_key");
        assertThat(prompt).contains("===DESCRIPTION===");
    }
}
