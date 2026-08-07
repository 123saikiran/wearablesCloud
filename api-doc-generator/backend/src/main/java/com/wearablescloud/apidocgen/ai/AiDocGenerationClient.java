package com.wearablescloud.apidocgen.ai;

/**
 * Abstraction over whichever LLM provider is configured via {@code ai.provider}.
 * Both methods take a fully-built prompt string and return the model's raw text
 * reply for the caller to interpret.
 */
public interface AiDocGenerationClient {

    String generateEndpointDoc(String prompt);

    String generateAuthExplanation(String prompt);
}
