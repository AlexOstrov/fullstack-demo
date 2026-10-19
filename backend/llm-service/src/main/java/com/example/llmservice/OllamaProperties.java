package com.example.llmservice;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "llm.ollama")
public record OllamaProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration chatTimeout,
        Duration tagsTimeout
) {
}
