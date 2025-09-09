package com.andrey.gpt.Config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ChatConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;


    @Value("${spring.ai.openai.chat.model:gpt-4o-mini}")
    private String model;


    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Bean
    public OpenAiApi openAiApi(WebClient.Builder webClientBuilder) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured (spring.ai.openai.api-key)");
        }

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16 MB buffer
                .build();

        WebClient.Builder wc = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                // Log request/response errors so you see JSON error bodies instead of EOF
                .filter(logOnError());

        return OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .webClientBuilder(wc)
                .build();
    }

    @Bean
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions opts = OpenAiChatOptions.builder()
                .model(model)
                .temperature(1.0)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(opts)
                .build();
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }

    private static ExchangeFilterFunction logOnError() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            if (resp.statusCode().isError()) {
                return resp.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            String logBody = body.isEmpty() ? "<empty body>" : body;
                            System.err.println("[OpenAI error] status=" + resp.statusCode() + " body=" + logBody);
                            return org.springframework.web.reactive.function.client.ClientResponse
                                    .create(resp.statusCode())
                                    .headers(h -> h.addAll(resp.headers().asHttpHeaders()))
                                    .cookies(c -> c.addAll(resp.cookies()))
                                    .body(body)
                                    .build();
                        });
            }
            return reactor.core.publisher.Mono.just(resp);
        });
    }
}