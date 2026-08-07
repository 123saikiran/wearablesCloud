package com.wearablescloud.apidocgen.service;

import com.wearablescloud.apidocgen.model.GeneratedEndpointDoc;
import com.wearablescloud.apidocgen.model.ParsedEndpoint;
import com.wearablescloud.apidocgen.model.ParsedSpec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRenderServiceTest {

    private final MarkdownRenderService service = new MarkdownRenderService();

    @Test
    void rendersHeaderPerEndpointAndAuthenticationSection() {
        ParsedEndpoint endpoint = new ParsedEndpoint(
                "/pets", "GET", "listPets", "List pets", null, List.of(), null, Map.of(), List.of());
        ParsedSpec spec = new ParsedSpec("Petstore", "1.0.0", "A test API", List.of(), List.of(endpoint), List.of());

        GeneratedEndpointDoc doc = new GeneratedEndpointDoc(
                "listPets", "Lists all pets.", "GET /pets", "[]", "restClient.get()...", "- 500: server error");

        String markdown = service.render(spec, Map.of("listPets", doc), "Use the api_key header.");

        assertThat(markdown).contains("# Petstore");
        assertThat(markdown).contains("## GET /pets");
        assertThat(markdown).contains("Lists all pets.");
        assertThat(markdown).contains("## Authentication");
        assertThat(markdown).contains("Use the api_key header.");
    }
}
