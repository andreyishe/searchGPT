package com.andrey.gpt.Services;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    public List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if(text==null || text.isEmpty()) {
            return chunks;
        }
        int startIndex = 0;
        int chunkIndex = 0;
        while(startIndex < text.length()) {
            int endIndex = Math.min(text.length(), startIndex + chunkSize);
            String chunk = text.substring(startIndex, endIndex).trim();
            chunks.add(chunk);

            System.out.println("chunk: " + chunkIndex+"-> ("+chunkSize+" size): "+chunk);
            chunkIndex++;
            startIndex = endIndex-overlap;
            if(startIndex < 0) {
                startIndex = 0;
            }
        }
        return chunks;
    }
}
