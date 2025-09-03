package com.andrey.gpt.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    private final OpenAiChatModel openAiChatModel;
    public ChatConfig(OpenAiChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }

    @Bean
    public ChatClient chatClient() {
        return ChatClient.create(openAiChatModel);
    }
}
