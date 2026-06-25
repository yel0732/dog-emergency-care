package com.ssafy.rescuemungz.emergencycheck;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gms")
public record GmsProperties(
        String apiKey,
        String baseUrl,
        String chatCompletionsPath,
        String model,
        int timeoutSeconds
) {
    public boolean configured() {
        return hasRealValue(apiKey)
                && hasRealValue(baseUrl)
                && hasRealValue(model);
    }

    public String normalizedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) return "";
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String normalizedChatCompletionsPath() {
        if (chatCompletionsPath == null || chatCompletionsPath.isBlank()) {
            return "/api.openai.com/v1/chat/completions";
        }
        return chatCompletionsPath.startsWith("/") ? chatCompletionsPath : "/" + chatCompletionsPath;
    }

    private boolean hasRealValue(String value) {
        if (value == null || value.isBlank()) return false;
        String lower = value.toLowerCase();
        return !lower.contains("put-your")
                && !lower.contains("your-")
                && !lower.contains("placeholder")
                && !lower.contains("disabled")
                && !lower.contains("api-key")
                && !lower.contains("api_key")
                && !lower.contains("here");
    }
}
