package com.andrey.gpt.Services;

import com.andrey.gpt.dto.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

@Service
public class GPTService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "https://api.openai.com/v1/chat/completions";

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatResponse getChatCompletion(String prompt) {
        try {
            // создаём тело запроса как Map
            Map<String, Object> body = new HashMap<>();
            body.put("model", "gpt-5");
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);
            body.put("messages", messages);

            String jsonBody = objectMapper.writeValueAsString(body); // безопасный JSON

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    ChatResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setError("Error: " + e.getMessage());
            return errorResponse;
        }
    }
}
