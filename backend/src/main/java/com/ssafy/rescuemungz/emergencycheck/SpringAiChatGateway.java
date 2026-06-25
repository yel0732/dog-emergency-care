package com.ssafy.rescuemungz.emergencycheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class SpringAiChatGateway implements AiChatGateway {
    private static final Logger log = LoggerFactory.getLogger(SpringAiChatGateway.class);
    private static final String SYSTEM_PROMPT = """
            You are a cautious veterinary triage assistant.
            Return only valid JSON.
            Do not diagnose, prescribe, or invent facts.
            """;

    private final GmsProperties properties;
    private final ChatClient chatClient;

    SpringAiChatGateway(GmsProperties properties, ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.properties = properties;
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.defaultSystem(SYSTEM_PROMPT).build();
    }

    @Override
    public Optional<String> completeJson(String userPrompt) {
        if (!properties.configured() || chatClient == null) {
            return Optional.empty();
        }
        try {
            String content = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                log.warn("GMS Spring AI response did not contain assistant text.");
                return Optional.empty();
            }
            return Optional.of(content);
        } catch (Exception ex) {
            log.warn("GMS Spring AI request could not be completed. reason={}", ex.toString());
            return Optional.empty();
        }
    }
}
