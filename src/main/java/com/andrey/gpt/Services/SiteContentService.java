package com.andrey.gpt.Services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SiteContentService {
    @Value("${site.url}")
    private String siteUrl;
    private String siteText;

    @PostConstruct
    public void init() {
        try {
            Document doc = Jsoup.connect(siteUrl).get();
            siteText = doc.text();
            System.out.println("Site content uploaded."+ siteText.length());
        }catch (Exception e) {
            e.printStackTrace();
            siteText = "";
        }
    }
    public String getSiteText() {
        return siteText;
    }
}
