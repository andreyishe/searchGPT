package com.andrey.gpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SearchGptApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchGptApplication.class, args);
        // в методе main до запуска SpringApplication.run(...)
        System.clearProperty("spring.ai.openai.chat.options.response-format");
        System.clearProperty("spring.ai.openai.chat.options.response-format.type");
        System.clearProperty("spring.ai.openai.chat.options.response-format.json-schema");

    }

}
