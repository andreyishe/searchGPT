package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class GPTService {

    private final ChatClient chatClient;
    private final Logger log = LoggerFactory.getLogger(GPTService.class);

    @Value("${spring.ai.openai.chat.model:gpt-4o-mini}")
    private String model;

    public GPTService(ChatClient.Builder builder,
        @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model){
           this.chatClient = builder.defaultOptions(
                   OpenAiChatOptions.builder()
                           .model(model)
                           .temperature(1.0)
                           .maxTokens(800)
                           .build()).build();
        }


    public ChatResponse getChatCompletion(String fullPrompt) {
        ChatResponse chatResponse = new ChatResponse();
        try {

            String content = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();

            if (content == null || content.isEmpty()) {
                chatResponse.setError("OpenAI returned empty response");
                return chatResponse;
            }

            ChatResponse.Choice choice = new ChatResponse.Choice();
            ChatResponse.Message message = new ChatResponse.Message();
            message.setContent(content);
            choice.setMessage(message);
            chatResponse.setChoices(List.of(choice));
            return chatResponse;

        } catch (RestClientResponseException http) {
            // Critical: expose upstream JSON error text for debugging
            String body = http.getResponseBodyAsString();
            log.error("OpenAI HTTP error {}: {}", http.getRawStatusCode(), body);
            chatResponse.setError("OpenAI error " + http.getRawStatusCode() + ": " + body);
            return chatResponse;
        } catch (RestClientException e){
            log.error("OpenAI error {}", e.getMessage());
            chatResponse.setError("OpenAI error " + e.getMessage());
            return chatResponse;
        } catch (Exception e) {
            log.error("Error calling OpenAI API", e);
            chatResponse.setError("Error calling OpenAI API: " + e.getMessage());
            return chatResponse;
        }
    }

    public ChatResponse getChatCompletionFromChunks(List<ContentChunk> chunks, String question) {
        // Defensive: limit context size (prevents 413/overlong bodies)
        final int maxChars = 12000;
        String context = chunks.stream()
                .map(ContentChunk::getText)
                .reduce((a, b) -> a + "\n---\n" + b)
                .orElse("No available content.");

        if (context.length() > maxChars) {
            context = context.substring(0, maxChars) + "\n...[truncated]...";
        }

        String fullPrompt = """
                You are an assistant who always answers with sources or links.
                Use the following context to answer the question.

                Context:
                %s

                Question: %s
                """.formatted(context, question);

        return getChatCompletion(fullPrompt);
    }
}