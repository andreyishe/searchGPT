package com.andrey.gpt.Services;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ChunkingService {


    private static final int MAX_WORDS_PER_CHUNK = 500;

    public List<String> chunkText(String text, int maxWords, int overlapWords) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        String[] words = text.split("\\s+");
        int start = 0;

        while (start < words.length) {
            int end = Math.min(words.length, start + maxWords);

            int safeEnd = Math.min(end, start + MAX_WORDS_PER_CHUNK);
            String chunk = String.join(" ", Arrays.copyOfRange(words, start, safeEnd)).trim();

            if (!chunk.isEmpty()) chunks.add(chunk);

            start = end - overlapWords;
            if (start < 0) start = 0;
        }
        return chunks;
    }
}
