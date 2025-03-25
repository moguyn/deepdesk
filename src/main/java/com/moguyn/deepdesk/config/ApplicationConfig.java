package com.moguyn.deepdesk.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.moguyn.deepdesk.advisor.AdvisorService;
import com.moguyn.deepdesk.chat.ChatRunner;
import com.moguyn.deepdesk.chat.CommandlineChatRunner;
import com.moguyn.deepdesk.dependency.SoftwareDependencyValidator;
import com.moguyn.deepdesk.tools.DateTimeTools;
import com.moguyn.deepdesk.tools.FilepathTools;
import com.moguyn.deepdesk.tools.ModelFriendlyExceptionProcessor;
import com.moguyn.deepdesk.tools.SyncMcpToolAdapter;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Application configuration to enable property binding
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CoreSettings.class)
public class ApplicationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "core.ui", name = "type", havingValue = "cli")
    public CommandLineRunner cli(
            ChatRunner chatManager,
            ConfigurableApplicationContext context) {
        return args -> {
            try (context) {
                chatManager.run(args);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Bean
    public AbstractChatMemoryAdvisor<VectorStore> chatMemoryAdvisor(VectorStore vectorStore,
            @Value("${core.llm.history-window-size}") int historyWindowSize) {
        return VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .chatMemoryRetrieveSize(historyWindowSize)
                .build();
    }

    @Bean
    public ChatRunner chatRunner(ChatClient chatClient) {
        return new CommandlineChatRunner(chatClient);
    }

    @PostConstruct
    public void dependencyValidation() {
        var validator = new SoftwareDependencyValidator();
        validator.verifyDependencies();
    }

    @Primary
    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(List<McpSyncClient> mcpClients) {
        return () -> mcpClients.stream()
                .flatMap(client -> client.listTools().tools().stream()
                .map(tool -> new SyncMcpToolAdapter(client, tool)))
                .toArray(ToolCallback[]::new);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
            ToolCallbackProvider toolCallbackProvider,
            AdvisorService advisorService,
            CoreSettings coreSettings,
            @Value("${core.llm.prompt.system}") String systemPrompt) {

        var builder = chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultTools(toolCallbackProvider)
                .defaultTools(new DateTimeTools(), new FilepathTools());

        // Get advisors from the service
        List<Advisor> enabledAdvisors = advisorService.getEnabledAdvisors(coreSettings);

        // Apply the enabled advisors
        if (!enabledAdvisors.isEmpty()) {
            builder.defaultAdvisors(enabledAdvisors.toArray(Advisor[]::new));
        } else {
            log.warn("No advisors enabled, chat client will operate without advisors");
        }

        return builder.build();
    }

    @Bean
    public VectorStore chromaVectorStore(EmbeddingModel embeddingModel, ChromaApi chromaApi,
            @Value("${spring.ai.vectorstore.chroma.collection-name}") String collectionName,
            @Value("${spring.ai.vectorstore.chroma.initialize-schema}") boolean initializeSchema) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(initializeSchema)
                .build();
    }

    /**
     * This is used to prevent the tool execution from being interrupted by an
     * exception.
     */
    @Bean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new ModelFriendlyExceptionProcessor();
    }

    @Bean
    public McpSyncClientCustomizer customMcpSyncClientCustomizer(@Value("#{'${core.mcp.roots}'.split(',')}") String[] roots) {
        return new CustomMcpSyncClientCustomizer(roots);
    }

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }
}
