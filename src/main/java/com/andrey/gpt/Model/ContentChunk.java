package com.andrey.gpt.Model;

public class ContentChunk {
    private String url;
    private String chunkId;
    private String text;

    public ContentChunk() {
    }

    public ContentChunk(String url, String chunkId, String text) {
        this.url = url;
        this.chunkId = chunkId;
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
