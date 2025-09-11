package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GPTService {

    private final ChatClient chatClient;
    private final Logger log = LoggerFactory.getLogger(GPTService.class);

    public GPTService(ChatClient.Builder builder,
                      @Value("${spring.ai.openai.chat.options.model:gpt-5}") String model) {

        String jsonSchema = """
        {
          "type": "object",
          "properties": {
            "answer": { "type": "string" }
          },
          "required": ["answer"],
          "additionalProperties": false
        }
        """;

        this.chatClient = builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, jsonSchema))
                        .temperature(1.0)
                        .maxCompletionTokens(800)
                        .build())
                .build();
    }

    public ChatResponse getChatCompletion(String fullPrompt) {
        ChatResponse response = new ChatResponse();
        try {
            log.info(">>> Sending prompt with JSON schema requirement: {}", fullPrompt);

            String content = chatClient.prompt()
                    .user(fullPrompt)
                    .call()
                    .content();

            log.info("<<< OpenAI response: {}", content);

            ChatResponse.Choice choice = new ChatResponse.Choice();
            ChatResponse.Message message = new ChatResponse.Message();
            message.setContent(content);
            choice.setMessage(message);
            response.setChoices(List.of(choice));

        } catch (Exception e) {
            log.error("OpenAI client error: {}", e.getMessage(), e);
            response.setError("OpenAI error: " + e.getMessage());
        }
        return response;
    }

    public ChatResponse getChatCompletionFromChunks(List<ContentChunk> chunks, String question) {
        final int maxChars = 12000;

        String context = chunks.stream()
                .map(ContentChunk::getText)
                .reduce((a, b) -> a + "\n---\n" + b)
                .orElse("No available content.");

        if (context.length() > maxChars) {
            context = context.substring(0, maxChars) + "\n...[truncated]...";
        }

        String fullPrompt = """
                Provide your answer strictly as JSON.
                Use the schema: { "answer": string }

                Context:
                %s

                Question: %s
                """.formatted(context, question);

        return getChatCompletion(fullPrompt);
    }
}
