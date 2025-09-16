package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private final SiteContentService site;

    private static final int MAX_CHARS_PER_CHUNK = 3500;
    private static final int DEFAULT_TOPK = 20;

    private static final Set<String> CAREER_KEYS = Set.of(
            "career","careers","job","jobs","hiring","vacancy","vacancies","join","team",
            "karriere","stellen","stellenangebot","stellenangebote","bewerben","wir suchen"
    );

    private static final Set<String> RETAIL_KEYS = Set.of(
            "retail","e-commerce","commerce","b2b","b2c","liferay","dxp","shop","affiliate"
    );

    public RetrievalService(SiteContentService site) {
        this.site = site;
    }

    public List<ContentChunk> retrieve(String query, Integer topK) {
        int k = (topK == null || topK <= 0) ? DEFAULT_TOPK : topK;
        List<ContentChunk> all = site.getChunks();
        if (all == null || all.isEmpty()) return List.of();

        Set<String> qTokens = tokens(query);


        List<Map.Entry<Integer, ContentChunk>> scored = new ArrayList<>();
        for (ContentChunk c : all) {
            int s = score(c, qTokens, query);
            scored.add(Map.entry(s, c));
        }


        return scored.stream()
                .sorted((a,b) -> Integer.compare(b.getKey(), a.getKey()))
                .limit(k)
                .map(Map.Entry::getValue)
                .map(this::truncate)
                .collect(Collectors.toList());
    }

    private ContentChunk truncate(ContentChunk c) {
        String t = Optional.ofNullable(c.getText()).orElse("");
        if (t.length() <= MAX_CHARS_PER_CHUNK) return c;
        return new ContentChunk(c.getUrl(), c.getChunkId(), t.substring(0, MAX_CHARS_PER_CHUNK) + "\n...[truncated]...");
    }

    private static Set<String> tokens(String s) {
        if (s == null) return Set.of();
        return Arrays.stream(s.toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{Nd}\\s]", " ")
                        .split("\\s+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }

    private static int score(ContentChunk c, Set<String> qTokens, String rawQuery) {
        String txt = Optional.ofNullable(c.getText()).orElse("").toLowerCase(Locale.ROOT);
        String url = Optional.ofNullable(c.getUrl()).orElse("").toLowerCase(Locale.ROOT);

        Set<String> t = tokens(txt);
        int overlap = (int) qTokens.stream().filter(t::contains).count();


        overlap += Math.min(t.size() / 200, 5);


        if (url.contains("karriere") || url.contains("career") || url.contains("jobs")) overlap += 6;
        if (url.contains("retail") || url.contains("e-commerce") || url.contains("branchen")) overlap += 2;


        String q = rawQuery.toLowerCase(Locale.ROOT);
        boolean careerIntent = CAREER_KEYS.stream().anyMatch(q::contains);
        boolean retailIntent  = RETAIL_KEYS.stream().anyMatch(q::contains);

        if (careerIntent) {
            if (txt.contains("karriere") || txt.contains("jobs") || txt.contains("bewerb")) overlap += 6;
        }
        if (retailIntent) {
            if (txt.contains("retail") || txt.contains("e-commerce") || txt.contains("liferay")) overlap += 3;
        }

        return overlap;
    }
}
