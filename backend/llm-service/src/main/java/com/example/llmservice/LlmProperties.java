package com.example.llmservice;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        String model,
        List<String> allowedModels,
        Duration refreshInterval
) {
    public boolean isAllowed(String model) {
        return model != null && allowedModels.contains(model);
    }
}
