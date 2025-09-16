package com.andrey.gpt.Services;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;

@Service
public class SiteCrawlerService {

    private static final Logger log = LoggerFactory.getLogger(SiteCrawlerService.class);

    private static final int PAGE_LOAD_TIMEOUT_SEC = 35;
    private static final int DOM_READY_TIMEOUT_SEC = 10;

    // поднимайте при необходимости
    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int DEFAULT_MAX_PAGES = 800;

    private final Set<String> visited = new LinkedHashSet<>();

    public Map<String, String> crawlSite(String startUrl, int maxDepth) {
        return crawlSite(startUrl, maxDepth, DEFAULT_MAX_PAGES);
    }

    public Map<String, String> crawlSite(String startUrl, int maxDepth, int maxPages) {
        visited.clear();
        Map<String, String> pages = new LinkedHashMap<>();
        WebDriver driver = null;

        try {
            String origin = normalizedOrigin(startUrl);
            if (origin.isEmpty()) {
                log.warn("Bad start URL: {}", startUrl);
                return pages;
            }

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                    "--disable-gpu", "--window-size=1920,1080");
            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(PAGE_LOAD_TIMEOUT_SEC));

            if (maxDepth <= 0) maxDepth = DEFAULT_MAX_DEPTH;

            log.info("Crawl start: {} | origin={} | maxDepth={} | maxPages={}",
                    startUrl, origin, maxDepth, maxPages);

            Deque<Node> q = new ArrayDeque<>();
            q.add(new Node(normalizeUrl(startUrl), 0));

            while (!q.isEmpty() && pages.size() < maxPages) {
                Node cur = q.pollFirst();
                if (cur == null) break;

                String url = cur.url();
                int depth = cur.depth();

                if (url.isBlank()) continue;
                if (depth > maxDepth) continue;
                if (!sameOrigin(url, origin)) continue;
                if (!visited.add(url)) continue;

                if (isPdf(url)) {
                    String pdfText = extractTextFromPdf(url);
                    if (!pdfText.isBlank()) {
                        pages.put(url, pdfText);
                        log.info("[{}] (pdf) OK, chars={}", url, pdfText.length());
                    }
                    continue;
                }

                try {
                    driver.get(url);
                    waitDomReady(driver);

                    String html = driver.getPageSource();
                    Document doc = Jsoup.parse(html, url);

                    String text = extractReadableText(doc);
                    if (!text.isBlank()) {
                        pages.put(url, text);
                        log.info("[{}] OK (depth={}), chars={}", url, depth, text.length());
                    } else {
                        log.info("[{}] EMPTY (depth={})", url, depth);
                    }

                    if (depth < maxDepth && pages.size() < maxPages) {
                        int added = 0;
                        for (Element a : doc.select("a[href]")) {
                            String href = a.attr("abs:href");
                            String child = normalizeUrl(href);
                            if (child.isBlank()) continue;
                            if (!sameOrigin(child, origin)) continue;
                            if (isSkippable(child)) continue;
                            if (!visited.contains(child)) {
                                q.addLast(new Node(child, depth + 1));
                                added++;
                            }
                        }
                        log.debug("[{}] queued {} links", url, added);
                    }

                } catch (Exception e) {
                    log.warn("Error while crawling {} : {}", url, e.toString());
                }
            }

            log.info("Crawling finished. Visited={} | Collected={}", visited.size(), pages.size());
        } catch (Exception e) {
            log.error("Crawler failed: {}", e.toString(), e);
        } finally {
            if (driver != null) {
                try { driver.quit(); } catch (Exception ignore) {}
            }
        }
        return pages;
    }

    // ===== helpers =====

    private void waitDomReady(WebDriver driver) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < DOM_READY_TIMEOUT_SEC * 1000L) {
            try {
                Object ready = ((JavascriptExecutor) driver).executeScript("return document.readyState");
                if ("complete".equals(ready)) return;
                Thread.sleep(150);
            } catch (Exception ignore) {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            }
        }
    }

    /** Храним полный путь + query, удаляем только фрагмент. */
    private String normalizeUrl(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            URI u = new URI(raw);
            // убираем фрагмент
            u = new URI(u.getScheme(), u.getUserInfo(), u.getHost(), u.getPort(), u.getPath(), u.getQuery(), null);
            String scheme = Optional.ofNullable(u.getScheme()).orElse("https").toLowerCase(Locale.ROOT);
            String host = Optional.ofNullable(u.getHost()).orElse("").toLowerCase(Locale.ROOT);
            if (host.isEmpty()) return "";
            String path = Optional.ofNullable(u.getRawPath()).orElse("");
            String query = Optional.ofNullable(u.getRawQuery()).map(q -> "?" + q).orElse("");
            return scheme + "://" + host + path + query;
        } catch (Exception e) {
            log.debug("normalizeUrl failed for '{}': {}", raw, e.toString());
            return "";
        }
    }

    /** Нормализованный origin: схема + host без ведущего www. */
    private String normalizedOrigin(String anyUrl) {
        try {
            URI u = new URI(anyUrl);
            String scheme = Optional.ofNullable(u.getScheme()).orElse("https").toLowerCase(Locale.ROOT);
            String host = Optional.ofNullable(u.getHost()).orElse("").toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            return host.isEmpty() ? "" : scheme + "://" + host;
        } catch (Exception e) {
            return "";
        }
    }

    /** Разрешаем www.{host} и без www. */
    private boolean sameOrigin(String url, String origin) {
        try {
            URI u = new URI(url);
            String scheme = Optional.ofNullable(u.getScheme()).orElse("https").toLowerCase(Locale.ROOT);
            String host = Optional.ofNullable(u.getHost()).orElse("").toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) host = host.substring(4);
            String lhs = scheme + "://" + host;
            return lhs.equals(origin);
        } catch (Exception e) {
            return url.startsWith(origin);
        }
    }

    private boolean isPdf(String url) {
        return url.toLowerCase(Locale.ROOT).contains(".pdf");
    }

    private boolean isSkippable(String url) {
        String u = url.toLowerCase(Locale.ROOT).trim();
        if (u.startsWith("mailto:") || u.startsWith("tel:") || u.startsWith("javascript:")) return true;
        if (u.endsWith("#")) return true;
        return u.matches(".*\\.(png|jpe?g|gif|svg|webp|ico|bmp|mp4|mp3|zip|rar|gz|7z|tar|docx?|xlsx?|pptx?)$");
    }

    private String extractTextFromPdf(String url) {
        try (InputStream in = new URL(url).openStream()) {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata meta = new Metadata();
            AutoDetectParser parser = new AutoDetectParser();
            ParseContext ctx = new ParseContext();
            parser.parse(in, handler, meta, ctx);
            return handler.toString();
        } catch (Exception e) {
            log.warn("PDF extract failed: {}", url, e.toString());
            return "";
        }
    }

    private String extractReadableText(Document doc) {
        doc.select("script, style, noscript, iframe, header .nav, nav, footer, aside").remove();

        StringBuilder sb = new StringBuilder(8192);

        doc.select("main, article, section").forEach(el -> {
            String t = el.text().trim();
            if (!t.isBlank()) sb.append(t).append('\n');
        });

        doc.select("h1, h2, h3, h4, p, li").forEach(el -> {
            String t = el.text().trim();
            if (!t.isBlank()) sb.append(t).append('\n');
        });

        doc.select("table tr").forEach(tr -> {
            String t = tr.text().trim();
            if (!t.isBlank()) sb.append(t).append('\n');
        });

        String out = sb.toString().replaceAll("\\s{2,}", " ").trim();
        if (out.length() > 60_000) out = out.substring(0, 60_000) + "\n...[truncated]...";
        return out;
    }

    private record Node(String url, int depth) {}
}
