package com.andrey.gpt.Components;

import com.andrey.gpt.Model.ContentChunk;
import com.andrey.gpt.Services.GPTService;
import com.andrey.gpt.Services.RetrievalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

@Component
@Profile("cli")
public class ChatCli implements CommandLineRunner {

    private final GPTService gptService;
    private final RetrievalService retrievalService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatCli(GPTService gptService, RetrievalService retrievalService) {
        this.gptService = gptService;
        this.retrievalService = retrievalService;
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("OpenAI CLI chat (RAG, always JSON) ====");

        while (true) {
            System.out.print("Enter request or 'exit' to quit: ");
            String input = scanner.nextLine();
            if (input == null) continue;
            if (input.equalsIgnoreCase("exit")) break;
            input = input.trim();
            if (input.isEmpty()) continue;

            try {
                // 1) достаём релевантные чанки из собранного контента
                List<ContentChunk> chunks = retrievalService.retrieve(input, 8);
                System.out.printf("[debug] retrieved %d context chunk(s)%n", chunks.size());

                // 2) спрашиваем модель С УЧЁТОМ контекста
                String json = gptService.getChatCompletionFromChunks(chunks, input);

                // 3) печатаем аккуратный ответ
                JsonNode node = mapper.readTree(json);
                System.out.println("\nAnswer:\n" + node.path("answer").asText());
                System.out.println("\nSources:");
                if (node.has("sources") && node.get("sources").isArray()) {
                    node.get("sources").forEach(s -> System.out.println(" - " + s.asText()));
                }
                System.out.println();
            } catch (Exception e) {
                System.out.println("\n[error] Failed to get completion: " + e.getMessage() + "\n");
            }
        }
    }
}
