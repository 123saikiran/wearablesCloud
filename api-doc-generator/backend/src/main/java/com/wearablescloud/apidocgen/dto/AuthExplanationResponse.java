package com.wearablescloud.apidocgen.dto;

import com.wearablescloud.apidocgen.model.SecurityScheme;

import java.util.List;

public record AuthExplanationResponse(List<SecurityScheme> schemes, String explanation) {
}
