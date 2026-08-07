package com.wearablescloud.apidocgen.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record PostmanExportResponse(ObjectNode collection, String summary) {
}
