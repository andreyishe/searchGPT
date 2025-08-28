package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private final SiteContentService site;

    public RetrievalService(SiteContentService site) {
        this.site = site;
    }

    public List<ContentChunk> retrieve(String query, Integer topK) {
        int k = (topK == null || topK <= 0) ? 8 : topK;

        Set<String> qTokens = tokens(query);
        List<ContentChunk> allChunks = site.getChunks();

        return allChunks.stream()
                .map(c -> Map.entry(score(c, qTokens), c))
                .sorted((a, b) -> Integer.compare(b.getKey(), a.getKey())) // по убыванию релевантности
                .limit(k)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private static Set<String> tokens(String s) {
        return Arrays.stream(s.toUpperCase()
                        .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                        .split("\\s+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }

    private static int score(ContentChunk c, Set<String> qTokens) {
        Set<String> t = tokens(c.getText());
        return (int) qTokens.stream().filter(t::contains).count();
    }
}
