package com.moguyn.deepdesk.chat;

import java.io.PrintStream;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandlineChatRunner implements ChatRunner {

    private final ChatClient chatClient;
    private final PrintStream console;

    public CommandlineChatRunner(ChatClient chatClient) {
        this.chatClient = chatClient;
        this.console = System.out;
    }

    @Override
    public void run(String... args) {
        console.println("\n我是您的AI助手，退出请键入 bye 或 exit\n");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                try {
                    String prompt = getUserInput(scanner);
                    if (shouldExit(prompt)) {
                        break;
                    }
                    if (shouldContinue(prompt)) {
                        continue;
                    }
                    String reply = promptAI(prompt);
                    console.println("AI: " + reply);
                } catch (Exception e) {
                    log.error("Error running chat", e);
                    console.println("系统: 发生错误了");
                }
            }
        }
    }

    private String getUserInput(Scanner scanner) {
        console.print("\n我: ");
        return scanner.nextLine();
    }

    private boolean shouldExit(String prompt) {
        return "exit".equalsIgnoreCase(prompt) || "bye".equalsIgnoreCase(prompt);
    }

    private boolean shouldContinue(String prompt) {
        return prompt == null || prompt.isEmpty();
    }

    private String promptAI(String prompt) {
        return chatClient.prompt(prompt)
                .call()
                .content();
    }
}
