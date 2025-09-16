package com.andrey.gpt.Controllers;

import com.andrey.gpt.Services.GPTService;
import com.andrey.gpt.Services.RetrievalService;
import com.andrey.gpt.Services.SiteContentService;
import com.andrey.gpt.Model.ContentChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final GPTService gptService;
    private final RetrievalService retrievalService;
    private final SiteContentService siteContentService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatController(GPTService gptService, RetrievalService retrievalService, SiteContentService siteContentService) {
        this.gptService = gptService;
        this.retrievalService = retrievalService;
        this.siteContentService = siteContentService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body) {
        final String prompt = body.getOrDefault("prompt", "").trim();
        if (prompt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "prompt is required"));
        }

        try {
            // Реально поднимаем topK, чтобы охватить больше фактов
            List<ContentChunk> relevant = retrievalService.retrieve(prompt, 30);

            // Falls back: если ретрив ничего не дал, возьмём 10 самых длинных страниц сайта
            if (relevant.isEmpty()) {
                List<ContentChunk> all = siteContentService.getChunks();
                relevant = all.stream()
                        .collect(Collectors.groupingBy(ContentChunk::getUrl))
                        .values().stream()
                        .map(list -> list.stream().findFirst().orElse(null))
                        .filter(Objects::nonNull)
                        .sorted((a,b) -> Integer.compare(
                                Optional.ofNullable(b.getText()).orElse("").length(),
                                Optional.ofNullable(a.getText()).orElse("").length()))
                        .limit(10)
                        .collect(Collectors.toList());
                log.warn("Retriever returned 0 chunks. Using fallback longest pages: {}",
                        relevant.stream().map(ContentChunk::getUrl).distinct().toList());
            }

            log.debug("Sending {} chunks, {} distinct URLs",
                    relevant.size(), relevant.stream().map(ContentChunk::getUrl).distinct().count());

            String json = gptService.getChatCompletionFromChunks(relevant, prompt);
            Map<?, ?> payload = mapper.readValue(json, Map.class);
            return ResponseEntity.ok(payload);

        } catch (RestClientResponseException http) {
            log.error("Upstream error: status={}, body={}", http.getRawStatusCode(), http.getResponseBodyAsString());
            return ResponseEntity.status(http.getRawStatusCode())
                    .body(Map.of("error", "Upstream error", "details", http.getResponseBodyAsString()));
        } catch (Exception e) {
            log.error("Chat endpoint failed", e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal error", "details", e.getMessage()));
        }
    }

    @GetMapping(value = "/site-content", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getSiteContent() {
        String siteText = siteContentService.getSiteText();
        return ResponseEntity.ok(siteText);
    }
}
