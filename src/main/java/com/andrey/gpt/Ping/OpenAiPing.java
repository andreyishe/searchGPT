package com.andrey.gpt.Ping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Profile("cli")
class OpenAiPing implements org.springframework.boot.ApplicationRunner{
    private final WebClient webClient;
    private final String apiKey;

    OpenAiPing(WebClient.Builder builder, @Value("{spring.ai.openai.api-key}") String apiKey) {
        this.webClient = builder.baseUrl("https://api.openai.api").build();
        this.apiKey = apiKey;
    }
    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        try {


            String body = webClient.get().uri("/v1/models")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(java.time.Duration.ofSeconds(20));
            System.out.println("[Ping] /v1/models OK, bytes=" + (body == null ? 0 : body.length()));
        } catch (Exception e) {
            System.out.println("[Ping] /v1/models FAILED"+e.getMessage());
        }
    }
}
