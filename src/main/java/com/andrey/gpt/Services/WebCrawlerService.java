package com.andrey.gpt.Services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class WebCrawlerService {
    private static final Logger logger = LoggerFactory.getLogger(WebCrawlerService.class);
    private Set<String> crawledUrls = new HashSet<>();
    private static final int MAX_DEPTH = 5;

    private void startCrawling(String startUrl) {
        logger.info("Starting crawling");
        crawlPage(startUrl, 0, getDomain(startUrl));
    }
    private void crawlPage(String url, int depth, String domain) {
        if(depth > MAX_DEPTH) {
            logger.info("Crawler has reached max depth at: "+url);
            return;
        }
        if(crawledUrls.contains(url)) {
            logger.debug("Crawler is already crawled");
            return;
        }
        try{
            logger.info("Crawling (depth{}): ",depth,url);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; CrawlerBot/1.0)")
                    .timeout(10000)
                    .get();
            crawledUrls.add(url);

            String text = doc.body().text();
            logger.info("Extracted text ({} chars) from {}", text.length(), url);

            Elements links = doc.select("a[href]");
            for(Element link : links) {
                String absUrl = link.absUrl("href");
                if(absUrl.contains(domain)) {
                    crawlPage(absUrl, depth+1, domain);
                }
            }

        } catch (IOException e) {
            logger.error("Error while crawling page {}: {}", url, e.getMessage());
        }

    }
    private String getDomain(String url) {
        try {
            String domain = url.split("/")[2];
            logger.debug("Detected domain: {}", domain);
            return domain;
        } catch (Exception e) {
            logger.error("Failed to parse domain from url {}", url);
            return "";
        }
    }

}
