package com.andrey.gpt.Components;

import com.openai.services.blocking.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ChatCli implements CommandLineRunner {
    private final ChatClient chatClient;

    public ChatCli(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("OpenAI CLI chat(including links) ====");

        while (true) {
            System.out.print("Enter request or 'exit' to quit: ");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) {
                break;
            }

            String response = chatClient.prompt()
                    .user("""
                        You are an assistant who always answers with sources or links.
                        In your answer, be sure to add links to materials, articles or official resources.
                        Question: %s
                        """.formatted(input))
                    .call()
                    .content();

            System.out.println("Response: " + response);
        }
    }
}
