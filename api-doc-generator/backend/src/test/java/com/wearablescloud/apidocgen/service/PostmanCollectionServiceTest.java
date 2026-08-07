package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import com.wearablescloud.apidocgen.model.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostmanCollectionServiceTest {

    private final PostmanCollectionService service = new PostmanCollectionService(new ObjectMapper());

    @Test
    void buildsCollectionWithOneItemPerEndpointAndCorrectSchemaVersion() {
        ParsedEndpoint listPets = new ParsedEndpoint(
                "/pets", "GET", "listPets", "List pets", null, List.of(), null, Map.of(), List.of());
        ParsedEndpoint createPet = new ParsedEndpoint(
                "/pets", "POST", "createPet", "Create a pet", null, List.of(), null, Map.of(), List.of("api_key"));

        ParsedSpec spec = new ParsedSpec(
                "Petstore", "1.0.0", null, List.of("https://example.com/api"),
                List.of(listPets, createPet),
                List.of(new SecurityScheme("api_key", "apiKey", null, null, "header", null)));

        PostmanCollectionResult result = service.build(spec);
        JsonNode collection = result.collection();

        assertThat(collection.path("info").path("schema").asText())
                .isEqualTo("https://schema.getpostman.com/json/collection/v2.1.0/collection.json");
        assertThat(collection.path("item")).hasSize(2);
        assertThat(result.summary()).contains("2 requests");
    }
}
