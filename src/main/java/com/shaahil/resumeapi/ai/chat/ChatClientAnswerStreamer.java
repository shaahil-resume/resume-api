package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChatClientAnswerStreamer implements AnswerStreamer {

    private final ChatClient chatClient;
    private final AiProperties properties;

    public ChatClientAnswerStreamer(ChatClient.Builder chatClientBuilder, AiProperties properties) {
        this.chatClient = chatClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public Flux<String> stream(String systemMessage, String userMessage) {
        AiProperties.Chat chat = properties.chat();
        ChatOptions.Builder<?> options = ChatOptions.builder()
                .temperature(chat.temperature())
                .maxTokens(chat.maxTokens());
        return chatClient.prompt()
                .system(systemMessage)
                .user(userMessage)
                .options(options)
                .stream()
                .content();
    }
}
