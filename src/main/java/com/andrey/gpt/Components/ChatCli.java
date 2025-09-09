package com.andrey.gpt.Components;

import com.andrey.gpt.Services.GPTService;
import com.openai.services.blocking.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@Profile("cli")
public class ChatCli implements CommandLineRunner {
    private final GPTService gptService;

    public ChatCli(GPTService gptService) {
        this.gptService = gptService;
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

            var response = gptService.getChatCompletion("""
                        You are an assistant who always answers with sources or links.
                        In your answer, be sure to add links to materials, articles or official resources.
                        Question: %s
                        """.formatted(input));


            var choices = response.getChoices();
            String message = (choices != null && !choices.isEmpty())
                    ?choices.get(0).getMessage().getContent()
                    :response.getError();
            System.out.println("Response: " + message);

        }
    }
}
