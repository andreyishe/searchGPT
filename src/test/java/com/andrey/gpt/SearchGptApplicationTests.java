package com.andrey.gpt;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest(properties = "spring.ai.openai.api-key=test")
class SearchGptApplicationTests {

    @Autowired
    private ChatClient chatClient;

    @Test
    void contextLoads() {
        assertNotNull(chatClient);
    }

}
