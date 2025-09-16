package com.andrey.gpt.config;

import com.andrey.gpt.Services.SiteContentService;
import com.andrey.gpt.Services.SiteCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CrawlerBootstrap {

    private static final Logger log = LoggerFactory.getLogger(CrawlerBootstrap.class);

    @Bean
    ApplicationRunner crawlOnStartup(
            SiteCrawlerService crawler,
            SiteContentService siteContentService,
            @Value("${app.crawl.start-url:https://www.ancud.de/}") String startUrl,
            @Value("${app.crawl.max-depth:3}") int maxDepth,
            @Value("${app.crawl.max-pages:300}") int maxPages,
            @Value("${app.crawl.enabled:true}") boolean enabled
    ) {
        return args -> {
            if (!enabled) {
                log.warn("Crawling disabled (app.crawl.enabled=false) — пропускаю загрузку контента.");
                return;
            }
            try {


                log.info("Starting crawl: url={} depth={} pages={}", startUrl, maxDepth, maxPages);
                Map<String, String> pages = crawler.crawlSite(startUrl, maxDepth, maxPages);

                log.info("Crawl done: collected={} pages", pages.size());
                siteContentService.loadFrom(pages);

                if (pages.isEmpty()) {
                    log.warn("Crawler returned 0 pages — проверьте наличие Chrome/Chromium в системе и сетевой доступ.");
                }
            } catch (Exception e) {
                log.error("Crawl bootstrap failed: {}", e.toString(), e);
            }
        };
    }
}
