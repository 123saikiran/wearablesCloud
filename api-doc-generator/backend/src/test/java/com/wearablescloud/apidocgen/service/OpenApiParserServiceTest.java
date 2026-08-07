package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiParserServiceTest {

    private OpenApiParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new OpenApiParserService(new ObjectMapper());
    }

    @Test
    void parsesPetstoreSampleSpec() {
        ParsedSpec spec = parsePetstore();

        assertThat(spec.title()).isEqualTo("Swagger Petstore");
        assertThat(spec.endpoints()).hasSize(4);
        assertThat(spec.securitySchemes()).extracting("name")
                .containsExactlyInAnyOrder("petstore_auth", "api_key");
    }

    @Test
    void extractsKnownOperationWithMethodAndPath() {
        ParsedSpec spec = parsePetstore();

        Optional<ParsedEndpoint> getPetById = spec.endpoints().stream()
                .filter(e -> e.operationId().equals("getPetById"))
                .findFirst();

        assertThat(getPetById).isPresent();
        assertThat(getPetById.get().method()).isEqualTo("GET");
        assertThat(getPetById.get().path()).isEqualTo("/pets/{petId}");
        assertThat(getPetById.get().parameters()).hasSize(1);
        assertThat(getPetById.get().parameters().get(0).name()).isEqualTo("petId");
    }

    @Test
    void extractsSecurityRequirementsOnProtectedOperation() {
        ParsedSpec spec = parsePetstore();

        ParsedEndpoint createPet = spec.endpoints().stream()
                .filter(e -> e.operationId().equals("createPet"))
                .findFirst()
                .orElseThrow();

        assertThat(createPet.securitySchemeNames()).containsExactly("petstore_auth");
        assertThat(createPet.requestBodySchema()).isNotNull();
        assertThat(createPet.requestBodySchema().properties()).containsKey("name");
    }

    private ParsedSpec parsePetstore() {
        InputStream specStream = getClass().getClassLoader().getResourceAsStream("petstore.json");
        assertThat(specStream).isNotNull();
        return parserService.parse(specStream);
    }
}
