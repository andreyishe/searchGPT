package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.Model.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SiteContentService {

    private final SiteCrawlerService crawler;

    public SiteContentService(SiteCrawlerService crawler) {
        this.crawler = crawler;
    }


    public String getSiteText() {
        Map<String, String> pages = crawler.crawlSite("https://www.ancud.de", 10);
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : pages.entrySet()) {
            builder.append("URL: ").append(entry.getKey()).append("\n");
            builder.append(entry.getValue()).append("\n\n");
        }

        return builder.toString().trim();
    }

    public List<ContentChunk> getChunks() {
        String text = getSiteText();
        List<ContentChunk> chunks = new ArrayList<>();
        int chunkSize = 1400;
        int overlap = 200;
        int pos = 0, index = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + chunkSize, text.length());
            String slice = text.substring(pos, end).trim();
            if (!slice.isEmpty()) {
                chunks.add(new ContentChunk("site", "chunk#" + index++, slice));
            }
            pos = end - overlap;
            if (pos <= end) pos = end;
        }
        return chunks;
    }
}
