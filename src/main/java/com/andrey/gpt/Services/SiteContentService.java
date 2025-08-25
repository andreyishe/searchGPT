package com.andrey.gpt.Services;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.Model.PageContent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.security.MessageDigest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SiteContentService {
    @Value("${site.url}")
    private String siteUrl;
    @Value("${site.urls}")
    private String siteUrls;
    private String siteText;

    private static final int CHUNK_SIZE = 1400;
    private static final int OVERLAP = 200;
    private List<String> seedUrls = new ArrayList<>();

    @PostConstruct
    public void init() {
        if(siteUrls != null && !siteUrls.isBlank()) {
            seedUrls = Arrays.stream(siteUrls.split(","))
                    .map(String::trim)
            .collect(Collectors.toList());
        }
        else if(siteUrl != null && !siteUrl.isBlank()) {
            seedUrls = Arrays.asList(siteUrl.trim());
        }
        System.out.println("Seed Urls: "+seedUrls);
    }

    public List<String> getSeedUrls() {
        return seedUrls;
    }

    @Cacheable(cacheNames = "pages", key = "#url")
    public PageContent fetchPageContent(String url) {
        try{
            Document document = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(15000).get();
            String title = Optional.ofNullable(document.title()).orElse(url);
            String text = document.select("main").text();
            String checkSum = sha256(text);
            return new PageContent(title, text, url, Instant.now(), checkSum);
        } catch (IOException e) {
            throw new RuntimeException("Error Fetching Page Content"+ url, e);
        }
    }

        public List<ContentChunk> toChunks(PageContent page) {
            String text = page.getText().replaceAll("\\s", " ").trim();
            List<ContentChunk> chunks = new ArrayList<>();
            int pos = 0;
            int index = 0;
            while (pos < text.length()) {
                int end = Math.min(text.length(), pos + CHUNK_SIZE);
                if (end > text.length()) {
                    int lastSpace = text.lastIndexOf(" ", end);
                    if (lastSpace > pos + (CHUNK_SIZE / 2)) end = lastSpace;
                }
                String slice = text.substring(pos, end).trim();
                if (!slice.isEmpty()) {
                    chunks.add(new ContentChunk(page.getUrl(), page.getUrl() + "#" + index++, slice));
                }
                pos = Math.max(0, end+OVERLAP);
                if(pos==end) break;
            }
            return chunks;
        }
    private String sha256(String s) {

    }
    public String getSiteText() {
        return siteText;
    }
}
