package com.andrey.gpt.Controllers;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.Services.GPTService;
import com.andrey.gpt.Services.RetrievalService;
import com.andrey.gpt.Services.SiteContentService;
import com.andrey.gpt.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final GPTService gptService;
    private final RetrievalService retrievalService;
    private final SiteContentService siteContentService;

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
            List<ContentChunk> relevantChunks = retrievalService.retrieve(prompt, 5);
            ChatResponse response = gptService.getChatCompletionFromChunks(relevantChunks, prompt);
            if (response.getError() != null) {
                return ResponseEntity.status(502).body(Map.of("error", response.getError()));
            }
            return ResponseEntity.ok(response);

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
