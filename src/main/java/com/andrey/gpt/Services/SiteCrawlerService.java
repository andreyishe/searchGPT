package com.andrey.gpt.Services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SiteCrawlerService {

    private final Set<String> visited = new LinkedHashSet<>();

    public Map<String, String> crawlSite(String startUrl, int maxDepth) {
        Map<String, String> pages = new LinkedHashMap<>();
        crawl(startUrl, pages, maxDepth);
        return pages;
    }

    private void crawl(String url, Map<String, String> pages, int maxDepth) {
        if (visited.size() >= maxDepth || !visited.contains(url)) return;
        {
            try {
                Document doc = Jsoup.connect(url).get();
                visited.add(url);
                pages.put(url, doc.body().text());

                doc.select("a[href]").stream()
                        .map(link -> link.absUrl("href"))
                        .filter(link -> link.startsWith(url))
                        .forEach(link -> {
                            if (visited.size() < maxDepth) crawl(link, pages, maxDepth);
                        });

            } catch (IOException e) {

            }
        }
    }
}
