package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class GPTService {

    private static final Logger log = LoggerFactory.getLogger(GPTService.class);
    private static final ObjectMapper M = new ObjectMapper();

    private final RestClient rest;
    private final String model;
    private final Integer maxTokens;
    private final Double temperature;

    public GPTService(
            RestClient.Builder restBuilder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model,
            @Value("${spring.ai.openai.chat.options.max-completion-tokens:800}") Integer maxTokens,
            @Value("${spring.ai.openai.chat.options.temperature:0.0}") Double temperature // делаем детерминированнее
    ) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;

        this.rest = RestClient.builder()
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Accept-Encoding", "identity")
                .build();
    }


    public String getChatCompletion(String question) {
        return getChatCompletionFromChunks(List.of(), question);
    }

    /** ВАЖНО: передаем контекст одним сообщением JSON-списка {id,url,text} — это проще и надёжнее. */
    public String getChatCompletionFromChunks(List<ContentChunk> chunks, String question) {
        final String sys = """
Return ONLY JSON:
{"answer": string, "sources": string[]}
Rules:
- Use ONLY facts from the provided "context_items" (their .text fields).
- If insufficient, set: {"answer":"I don't know","sources":[]}.
- "sources" must be distinct URLs taken from "context_items[*].url" that you actually used.
No prose. No preamble.
""";


        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ContentChunk c = chunks.get(i);
            if (c == null) continue;
            String url = safe(c.getUrl());
            String txt = safe(c.getText());
            if (txt.isEmpty()) continue;

            if (txt.length() > 6000) txt = txt.substring(0, 6000) + "\n...[truncated]...";
            items.add(Map.of(
                    "id", "chunk#" + i,
                    "url", url,
                    "text", txt
            ));
        }

        Map<String, Object> userPayload = new LinkedHashMap<>();
        userPayload.put("instruction",
                "You are given a list \"context_items\", each item = {id, url, text}. " +
                        "Answer strictly and only using facts from \"context_items.text\". " +
                        "If the context is insufficient, return {\"answer\":\"I don't know\",\"sources\":[]}. " +
                        "Return only {\"answer\", \"sources\"} and nothing else.");
        userPayload.put("context_items", items);
        userPayload.put("question", question);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", sys),
                Map.of("role", "user", "content", toJson(userPayload))
        ));

        ResponseEntity<String> entity = rest.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        String raw = entity.getBody();
        if (raw == null || raw.isBlank()) {
            log.error("Empty body from /v1/chat/completions (status={})", entity.getStatusCode());
            return "{\"answer\":\"I don't know\",\"sources\":[]}";
        }
        return parse(raw);
    }



    private String parse(String raw) {
        try {
            JsonNode root = M.readTree(raw);
            JsonNode choices = root.path("choices");
            String content = null;
            if (choices.isArray() && choices.size() > 0) {
                content = choices.get(0).path("message").path("content").asText(null);
            }
            if (content == null || content.isBlank()) {
                log.error("No content in choices[0].message.content. head={}", raw.substring(0, Math.min(300, raw.length())));
                return "{\"answer\":\"I don't know\",\"sources\":[]}";
            }


            try {
                JsonNode obj = M.readTree(content);
                String answer = obj.path("answer").asText("");
                List<String> sources = new ArrayList<>();
                if (obj.has("sources") && obj.get("sources").isArray()) {
                    for (JsonNode n : (ArrayNode) obj.get("sources")) {
                        String s = n.asText(null);
                        if (s != null && !s.isBlank()) sources.add(s);
                    }
                }
                LinkedHashSet<String> distinct = new LinkedHashSet<>(sources);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("answer", answer);
                out.put("sources", new ArrayList<>(distinct));
                return M.writeValueAsString(out);
            } catch (Exception inner) {

                return M.writeValueAsString(Map.of("answer", content, "sources", List.of()));
            }
        } catch (Exception e) {
            log.error("Chat parse error", e);
            String esc = raw.replace("\"", "\\\"");
            return "{\"answer\":\"" + esc + "\",\"sources\":[]}";
        }
    }

    private static String toJson(Object o) {
        try { return M.writeValueAsString(o); } catch (Exception e) { return "{}"; }
    }
    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
