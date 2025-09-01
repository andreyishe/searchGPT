package com.andrey.gpt.Utils;

public class TokenUtils {

    public static String truncateByTokens(String text, int maxTokens) {
        if(text==null) return null;
        if(text.length()>maxTokens) return text;
        return text.substring(0, maxTokens) + "...";
    }
}
