package com.andrey.gpt.Model;

import java.time.Instant;

public class PageContent {
    private String url;
    private String title;
    private String text;
    private Instant fetchUp;
    private String checkSum;

    public PageContent() {}

    public PageContent(String url, String title, String text, Instant fetchUp, String checkSum) {
        this.url = url;
        this.title = title;
        this.text = text;
        this.fetchUp = fetchUp;
        this.checkSum = checkSum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getFetchUp() {
        return fetchUp;
    }

    public void setFetchUp(Instant fetchUp) {
        this.fetchUp = fetchUp;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }
}
