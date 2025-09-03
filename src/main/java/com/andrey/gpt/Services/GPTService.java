package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GPTService {

    private final ChatClient chatClient;
    private final Logger log = LoggerFactory.getLogger(GPTService.class);

    public GPTService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse getChatCompletion(String fullPrompt) {
        ChatResponse chatResponse = new ChatResponse();
        try {
            // Отправляем полный контекст + вопрос
            String content = chatClient.prompt()
                    .user("""
                        You are an assistant who always answers with sources or links.
                        Use the context to answer the question.
                        Context and question: %s
                        """.formatted(fullPrompt))
                    .call()
                    .content();

            ChatResponse.Choice choice = new ChatResponse.Choice();
            ChatResponse.Message message = new ChatResponse.Message();
            message.setContent(content);
            choice.setMessage(message);
            chatResponse.setChoices(List.of(choice));
            return chatResponse;

        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            chatResponse.setError("Error calling OpenAI API: " + e.getMessage());
            return chatResponse;
        }
    }

    // Удобный метод для работы с чанками из RetrievalService
    public ChatResponse getChatCompletionFromChunks(List<ContentChunk> chunks, String question) {
        String context = chunks.stream()
                .map(ContentChunk::getText)
                .reduce((a, b) -> a + "\n---\n" + b)
                .orElse("");

        String fullPrompt = "Use this text from the website:\n"
                + context
                + "\n\nQuestion: " + question;

        return getChatCompletion(fullPrompt);
    }
}
