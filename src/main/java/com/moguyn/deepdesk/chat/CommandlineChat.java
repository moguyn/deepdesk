package com.moguyn.deepdesk.chat;

import java.io.PrintStream;
import java.util.Scanner;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;

import com.moguyn.deepdesk.mcp.ToolManager;

public class CommandlineChat implements ChatManager {

    private final ChatClient chatClient;
    private final ToolManager toolManager;

    public CommandlineChat(ChatClient.Builder chatClientBuilder, ToolManager toolManager) {
        this.toolManager = toolManager;
        this.chatClient = chatClientBuilder
                .defaultSystem("你是企业级AI助手, 请说中文")
                .defaultTools(toolManager.loadTools())
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }

    @Override
    public void start() {
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

    @Override
    public void stop() {
        toolManager.shutdown();
    }

}
