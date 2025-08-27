package com.andrey.gpt.Services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.ContentHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

@Service
public class SiteCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(SiteCrawlerService.class);

    private final Set<String> visited = new LinkedHashSet<>();

    public SiteCrawlerService() {
        WebDriverManager.safaridriver().setup();
        WebDriverManager.chromedriver().setup();
        WebDriverManager.operadriver().setup();
    }

    public Map<String, String> crawlSite(String startUrl, int maxDepth) {
        Map<String, String> pages = new LinkedHashMap<>();
        try {
            URI uri = new URI(startUrl);
            String domain = uri.getScheme() + "://" + uri.getHost();
            WebDriver driver = new SafariDriver();
            logger.info("Starting crawl from: {} with maxDepth={}", startUrl, maxDepth);
            try {
                crawl(startUrl, domain, pages, maxDepth, 0, driver);
            } finally {
                driver.quit();
            }
            logger.info("Crawling finished. Total pages visited: {}", visited.size());

        } catch (URISyntaxException e) {
            logger.error("Invalid start URL: {}", startUrl, e);
        }
        return pages;
    }

    private String extractTitle(Document doc) {
        doc.select("script, nav, style, header, footer, noscript").remove();
        StringBuilder textBuilder = new StringBuilder();

        String mainText = doc.select("main").text();
        if (!mainText.isBlank()) textBuilder.append(mainText).append("\n");

        String articleText = doc.select("article").text();
        if (!articleText.isBlank()) textBuilder.append(articleText).append("\n");

        doc.select("p").forEach(p -> {
            String t = p.text().trim();
            if (t.length() > 20) textBuilder.append(t).append("\n");
        });

        return textBuilder.toString();
    }

    private void crawl(String url, String domain, Map<String, String> pages, int maxDepth, int depth, WebDriver driver) {
        if (visited.size() >= maxDepth || visited.contains(url)) return;

            logger.debug("Max depth {} reached at {}", maxDepth, url);
        try {
            url = normalizeUrl(url);
            visited.add(url);
            if (url.endsWith(".pdf")) {
                String pdfText = extractTextFromPdf(url);
                if (!pdfText.isBlank()) pages.put(url, pdfText);
            } else {
                driver.get(url);
                Thread.sleep(2000);
                String pageSource = driver.getPageSource();

                Document doc = Jsoup.parse(pageSource);
                String text = extractText(doc);
                if (!text.isBlank()) pages.put(url, text);
            }

            if (visited.contains(url)) {
                logger.debug("Already visited: {}", url);
                return;
            }

            if (isSkippable(url)) {
                logger.debug("Skipping binary file: {}", url);
                return;
            }

            logger.info("Crawling URL: {} (depth={})", url, depth);

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/115.0 Safari/537.36")
                    .timeout(15000)
                    .get();

            visited.add(url);

            String text = extractTitle(doc);
            if (!text.isBlank()) {
                pages.put(url, text);
                logger.debug("Extracted {} characters of text from {}", text.length(), url);
            } else {
                logger.debug("No useful text extracted from {}", url);
            }

            doc.select("a[href]").stream()
                    .map(link -> link.absUrl("href"))
                    .filter(link -> link.startsWith(domain))
                    .forEach(link -> crawl(link, domain, pages, maxDepth, depth + 1, driver));
        } catch (Exception e) {
            logger.error("Error while crawling {} : {}", url, e.getMessage());
        }
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            URI uri = new URI(url.split("#")[0]); // remove anchors (#)
            String cleanPath = (uri.getPath() != null && uri.getPath().endsWith("/"))
                    ? uri.getPath().substring(0, uri.getPath().length() - 1)
                    : uri.getPath();
            return uri.getScheme() + "://" + uri.getHost() + (cleanPath != null ? cleanPath : "");
        } catch (URISyntaxException e) {
            logger.warn("Could not normalize URL: {}", url);
            return url;
        }
    }
    private String extractTextFromPdf(String url) {
        try(InputStream in = new URL(url).openStream()) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(in, handler, metadata, null);
            return handler.toString();
        } catch (Exception e) {
            logger.error("Error while extracting text from pdf: {}", url, e);
            return "";
        }
    }

    private String extractText(Document doc) {
        doc.select("script, style, header, footer, noscript").remove();
        StringBuilder textBuilder = new StringBuilder();
        doc.select("main, article, section, div, span, p").forEach(el -> {
            String t = el.text().trim();
            if(!t.isBlank()) textBuilder.append(t).append("\n");
        });
        return textBuilder.toString();
    }
    private boolean isSkippable(String url) {
        return url.matches("(?i).*(\\.pdf|\\.png|\\.jpg|\\.jpeg|\\.gif|\\.svg|\\.docx?|\\.xlsx?|\\.pptx?)$");
    }
}
