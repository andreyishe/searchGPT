package com.andrey.gpt.Services;

import com.andrey.gpt.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class OpenAIChatService {

    private final ChatClient chatClient;

    public OpenAIChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse ask(String prompt) {
        try {
            String content = chatClient.prompt()
                    .user("Question: %s".formatted(prompt))
                    .call()
                    .content();

            ChatResponse chatResponse = new ChatResponse();
            ChatResponse.Choice choice = new ChatResponse.Choice();
            ChatResponse.Message message = new ChatResponse.Message();
            message.setContent(content);
            choice.setMessage(message);
            chatResponse.setChoices(List.of(choice));

            return chatResponse;
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setError("Error calling OpenAI API: " + e.getMessage());
            return errorResponse;
        }
    }
}

