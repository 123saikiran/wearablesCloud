package com.wearablescloud.apidocgen.model;

import java.util.List;
import java.util.Map;

public record ParsedSchema(
        String type,
        String format,
        String description,
        Map<String, ParsedSchema> properties,
        ParsedSchema items,
        List<String> required,
        String example
) {
}
