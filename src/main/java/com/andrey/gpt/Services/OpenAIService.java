package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;

@Service
public class OpenAIService {

    private final RetrievalService retrieval;

    // Настройки
    private static final int MAX_WORDS_PER_CHUNK = 500;
    private static final int MAX_TOTAL_TOKENS = 2000;
    private static final double TOKEN_PER_WORD = 1.3;

    public OpenAIService(RetrievalService retrieval) {
        this.retrieval = retrieval;
    }

    public String askGPT(String query) {
        List<ContentChunk> relevantChunks = retrieval.retrieve(query, 5);

        StringBuilder combinedResponse = new StringBuilder();
        int totalTokens = 0;

        for (ContentChunk chunk : relevantChunks) {
            String text = chunk.getText();
            if (text == null || text.isBlank()) continue;


            String safeChunk = trimChunk(text, MAX_WORDS_PER_CHUNK);


            int chunkTokens = (int) (safeChunk.split("\\s+").length * TOKEN_PER_WORD);

            if (totalTokens + chunkTokens > MAX_TOTAL_TOKENS) {

                break;
            }

            String response = callOpenAI(safeChunk, query);
            combinedResponse.append(response).append("\n");

            totalTokens += chunkTokens;
        }

        return combinedResponse.toString();
    }


    private String trimChunk(String text, int maxWords) {
        String[] words = text.split("\\s+");
        int end = Math.min(words.length, maxWords);
        return String.join(" ", Arrays.copyOfRange(words, 0, end));
    }


    private String callOpenAI(String chunkText, String query) {
        return "GPT response for chunk: " + chunkText.substring(0, Math.min(100, chunkText.length())) + "...";
    }
}
