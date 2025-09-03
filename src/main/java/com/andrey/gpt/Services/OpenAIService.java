package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.Utils.TokenUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;

@Service
public class OpenAIService {
    private final RetrievalService retrieval;
    private static final int MAX_CHARS_PER_CHUNK = 500;
    private static final int TopK = 5;

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


            String safeChunk = TokenUtils.truncateByTokens(text, MAX_CHARS_PER_CHUNK);


            String response = callOpenAI(safeChunk, query);
            combinedResponse.append(response).append("\n");
        }

        return combinedResponse.toString();
    }


    private String truncateChunk(String text, int maxCharsPerChunk) {
        if (text.length() <= maxCharsPerChunk) return text.substring(0, maxCharsPerChunk);
        return text.substring(0, maxCharsPerChunk) + "...";
    }


    private String callOpenAI(String chunkText, String query) {
        return "GPT response for chunk: " + query+" with text: " + chunkText.substring(0, Math.min(50, chunkText.length())) + "\n";
    }
}
