package com.moguyn.deepdesk.chat;

import java.io.PrintStream;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandlineChatRunner implements ChatRunner {

    private final ChatClient chatClient;

    public CommandlineChatRunner(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        PrintStream console = System.out;
        console.println("\n我是您的AI助手(退出请输入bye或者exit)\n");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                console.print("\n用户: ");
                String prompt = scanner.nextLine();
                if ("exit".equalsIgnoreCase(prompt) || "bye".equalsIgnoreCase(prompt)) {
                    break;
                }
                var reply = chatClient.prompt(prompt)
                        .call()
                        .content();
                console.println("AI: " + reply);
            }
        } catch (Exception e) {
            log.error("Error running chat", e);
        }
    }

}
