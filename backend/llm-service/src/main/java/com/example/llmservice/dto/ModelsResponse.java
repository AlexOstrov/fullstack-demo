package com.example.llmservice.dto;

import java.util.List;

public record ModelsResponse(
        List<String> models,
        String defaultModel
) {
}
