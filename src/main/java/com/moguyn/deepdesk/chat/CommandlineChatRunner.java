package com.moguyn.deepdesk.chat;

import java.io.PrintStream;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;

import com.moguyn.deepdesk.mcp.ToolManager;

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
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                console.print("\n用户: ");
                String prompt = scanner.nextLine();
                if ("exit".equalsIgnoreCase(prompt) || "bye".equalsIgnoreCase(prompt)) {
                    break;
                }
                console.println("\nAI: "
                        + chatClient.prompt(prompt)
                                .call()
                                .content());
            }
        }
        stop();
    }

    private void stop() {
        toolManager.shutdown();
    }

}
