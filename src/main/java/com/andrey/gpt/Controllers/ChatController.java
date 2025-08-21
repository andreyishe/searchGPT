package com.andrey.gpt.Controllers;

import com.andrey.gpt.Services.GPTService;
import com.andrey.gpt.dto.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final GPTService gptService;

    public ChatController(GPTService gptService) {
        this.gptService = gptService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody String prompt) {
        try {
            ChatResponse response = gptService.getChatCompletion(prompt);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(429)
                    .body(new ErrorResponse("Too many requests. Try again later."));
        }
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
