package com.moguyn.deepdesk.chat;

import java.io.PrintStream;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;

import com.moguyn.deepdesk.capability.ToolManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandlineChatRunner implements ChatRunner {

    private final ChatClient chatClient;
    private final ToolManager toolManager;

    public CommandlineChatRunner(ChatClient chatClient, ToolManager toolManager) {
        this.toolManager = toolManager;
        this.chatClient = chatClient;
    }

    @Override
    public void run(String... args) {
        PrintStream console = System.out;
        console.println("\n我是您的AI助手(退出请输入bye或者exit)\n");
        try (Scanner scanner = new Scanner(System.in); toolManager) {
            while (true) {
                console.print("\n用户: ");
                String prompt = scanner.nextLine();
                if ("exit".equalsIgnoreCase(prompt) || "bye".equalsIgnoreCase(prompt)) {
                    break;
                }
                console.print("AI: ");
                console.println(chatClient.prompt(prompt)
                        .call()
                        .content());
            }
        } catch (Exception e) {
            log.error("Error running chat", e);
        }
    }

}
