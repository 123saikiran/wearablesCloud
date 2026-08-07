package com.wearablescloud.apidocgen.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record PostmanCollectionResult(ObjectNode collection, String summary) {
}
