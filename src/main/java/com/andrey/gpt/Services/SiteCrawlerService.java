package com.andrey.gpt.Services;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
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
import java.time.Duration;
import java.util.*;

@Service
public class SiteCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(SiteCrawlerService.class);
    private final Set<String> visited = new LinkedHashSet<>();

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
    public Map<String, String> crawlSite(String startUrl, int maxDepth) {
        visited.clear();
        Map<String, String> pages = new LinkedHashMap<>();
        WebDriver driver = new ChromeDriver();
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception ignore) {}
            try {
                driver.quit();
            } catch (Exception ignore) {}
        }
        try {
            String domain = originOf(startUrl);


            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                    "--disable-gpu", "--window-size=1920,1080");


            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            logger.info("Starting crawl from {} (maxDepth={})", startUrl, maxDepth);
            crawl(startUrl, domain, pages, maxDepth, 0, driver);
            logger.info("Crawling finished. Pages visited: {}", visited.size());
        } catch (Exception e) {
            logger.error("Crawler failed: {}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignore) {}
            }
        }
        return pages;
    }
    private void crawl(String url, String domain, Map<String, String> pages, int maxDepth, int depth, WebDriver driver) {
        String normalized = normalizeUrl(url);
        if (normalized.isBlank()) return;
        if (depth > maxDepth) return;
        if (!sameOrigin(normalized, domain)) return;
        if (!visited.add(normalized)) return;
        if (isPdf(normalized)) {
            logger.info("PDF detected: {}", normalized);
            String pdfText = extractTextFromPdf(normalized);
            if (!pdfText.isBlank()) pages.put(normalized, pdfText);
            return;
        }
            logger.debug("Max depth {} reached at {}", maxDepth, url);
        try {
            logger.info("Crawling [{}] depth={}", normalized, depth);

            if (isPdf(normalized)) {
                String pdfText = extractTextFromPdf(normalized);
                if (!pdfText.isBlank()) pages.put(normalized, pdfText);
                return;
            }


            driver.get(normalized);
            String pageSource = driver.getPageSource();


            Document doc = Jsoup.parse(pageSource, normalized);


            String text = extractText(doc);
            if (!text.isBlank()) pages.put(normalized, text);


            for (Element a : doc.select("a[href]")) {
                String href = a.attr("abs:href");
                if (href == null || href.isBlank()) continue;

                String child = normalizeUrl(href);
                if (child.isBlank()) continue;
                if (!sameOrigin(child, domain)) continue;
                if (isSkippable(child)) continue;

                crawl(child, domain, pages, maxDepth, depth + 1, driver);
            }

        } catch (Exception e) {
            logger.error("Error while crawling {} : {}", normalized, e.getMessage());
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
        try (InputStream in = new URL(url).openStream()) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext context = new ParseContext();  // <- вот оно
            parser.parse(in, handler, metadata, context);  // не null
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

    private String originOf(String anyUrl) {
        try {
            URI u = new URI(anyUrl);
            return u.getScheme() + "://" + u.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isPdf(String url) {
        return url.toLowerCase().contains(".pdf");
    }

    private boolean sameOrigin(String url, String origin) {
        return url.startsWith(origin);
    }
}
