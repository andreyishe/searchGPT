package com.andrey.gpt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SearchGptApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchGptApplication.class, args);
    }

}
