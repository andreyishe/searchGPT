package com.andrey.gpt.Controllers;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.Services.GPTService;
import com.andrey.gpt.Services.RetrievalService;
import com.andrey.gpt.Services.SiteContentService;
import com.andrey.gpt.dto.ChatResponse;
import com.openai.services.blocking.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GPTService gptService;
    private final RetrievalService retrievalService;
    private final SiteContentService siteContentService;
    public ChatController(GPTService gptService, RetrievalService retrievalService, SiteContentService siteContentService) {
        this.gptService = gptService;
        this.retrievalService = retrievalService;
        this.siteContentService = siteContentService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody String prompt) {
        try {
            List<ContentChunk> relevantChunks = retrievalService.retrieve(prompt, 5);
            String context = relevantChunks.stream()
                    .map(ContentChunk::getText)
                    .collect(Collectors.joining("\n---\n"));



            String fullPrompt = "Use this text from the website :\n"
                    + context
                    + "\n\nQuestion: " + prompt;

            ChatResponse response = gptService.getChatCompletion(fullPrompt);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(429)
                    .body(new ErrorResponse("Too many requests. Try again later."));
        }
    }
    @GetMapping("/site-content")
    public ResponseEntity<?> getSiteContent() {
        String siteText = siteContentService.getSiteText();
        return ResponseEntity.ok(siteText);
    }

    static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
