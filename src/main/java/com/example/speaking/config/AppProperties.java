package com.example.speaking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String corsAllowedOrigin, Gemini gemini) {
    public record Gemini(String baseUrl, String apiKey, String model, String systemPrompt) {}
}
