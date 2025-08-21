package com.andrey.gpt.Services;

import com.andrey.gpt.dto.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

@Service
public class GPTService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiUrl = "https://api.openai.com/v1/chat/completions";
    private final String apiKey = System.getenv("OPENAI_API_KEY");

    public ChatResponse getChatCompletion(String prompt) {
        try {
            // тело запроса
            String requestBody = "{\n" +
                    "  \"model\": \"gpt-4o-mini\",\n" +
                    "  \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]\n" +
                    "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ChatResponse> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    ChatResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            // в случае ошибки возвращаем объект с сообщением
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setError("Error: " + e.getMessage());
            return errorResponse;
        }
    }
}
