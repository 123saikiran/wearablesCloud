package com.wearablescloud.apidocgen.dto;

import java.util.List;
import java.util.UUID;

public record UploadSpecResponse(
        UUID specId,
        String title,
        String version,
        int endpointCount,
        List<String> securitySchemeNames
) {
}
