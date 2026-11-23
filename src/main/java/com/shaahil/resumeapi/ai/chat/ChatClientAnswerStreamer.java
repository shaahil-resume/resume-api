package com.shaahil.resumeapi.ai.chat;

import com.shaahil.resumeapi.ai.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

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
        return stream(systemMessage, userMessage, usage -> {
        });
    }

    @Override
    public Flux<String> stream(String systemMessage, String userMessage, Consumer<TokenUsage> usageSink) {
        AiProperties.Chat chat = properties.chat();
        ChatOptions.Builder<?> options = ChatOptions.builder()
                .temperature(chat.temperature())
                .maxTokens(chat.maxTokens());
        return chatClient.prompt()
                .system(systemMessage)
                .user(userMessage)
                .options(options)
                .stream()
                .chatResponse()
                .mapNotNull(response -> {
                    reportUsage(response, usageSink);
                    return text(response);
                });
    }

    /** With stream usage on, the provider sends one last chunk with no text and the token counts. */
    private static void reportUsage(ChatResponse response, Consumer<TokenUsage> usageSink) {
        Usage usage = response.getMetadata().getUsage();
        int prompt = usage.getPromptTokens();
        int completion = usage.getCompletionTokens();
        if (prompt + completion > 0) {
            usageSink.accept(new TokenUsage(prompt, completion));
        }
    }

    private static String text(ChatResponse response) {
        Generation generation = response.getResult();
        return generation == null ? null : generation.getOutput().getText();
    }
}
