package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SiteContentService {

    private static final Logger log = LoggerFactory.getLogger(SiteContentService.class);

    private volatile Map<String, String> pages = new LinkedHashMap<>();
    private volatile boolean loaded = false;

    public synchronized void loadFrom(Map<String, String> crawled) {
        this.pages = (crawled == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(crawled);
        this.loaded = !this.pages.isEmpty();
        log.info("SiteContent loaded: {} page(s)", this.pages.size());
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<ContentChunk> getChunks() {
        if (!loaded || pages.isEmpty()) return List.of();
        List<ContentChunk> chunks = new ArrayList<>();
        final int maxCharsPerChunk = 4000;

        pages.forEach((url, text) -> {
            if (text == null || text.isBlank()) return;
            int idx = 0, part = 0;
            while (idx < text.length()) {
                int end = Math.min(idx + maxCharsPerChunk, text.length());
                String piece = text.substring(idx, end);
                chunks.add(new ContentChunk(url, url + "#chunk-" + part, piece));
                idx = end;
                part++;
            }
        });
        return chunks;
    }

    public String getSiteText() {
        if (!loaded || pages.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        pages.forEach((url, text) -> {
            sb.append("URL: ").append(url).append('\n')
                    .append(text).append("\n---\n");
        });
        return sb.toString();
    }
}
