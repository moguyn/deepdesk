package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.lang.NonNull;

import reactor.core.publisher.Flux;

public class TokenLimitedChatMemoryAdvisor extends AbstractChatMemoryAdvisor<ChatMemory> {

    private final ExcessiveContentTruncator<Message> excessiveContentTruncator;

    /**
     * Create a new TokenLimitedChatMemoryAdvisor with the specified memory,
     * conversationId, and excessive content truncator.
     *
     * @param chatMemory the chat memory to store messages
     * @param defaultConversationId the conversation ID
     * @param chatHistoryWindowSize the size of the chat history window
     * @param excessiveContentTruncator the truncator to use to truncate
     * excessive content
     */
    public TokenLimitedChatMemoryAdvisor(ChatMemory chatMemory, String defaultConversationId,
            int chatHistoryWindowSize, ExcessiveContentTruncator<Message> excessiveContentTruncator) {
        super(chatMemory, defaultConversationId, chatHistoryWindowSize, true);
        this.excessiveContentTruncator = excessiveContentTruncator;
    }

    public static Builder builder(ChatMemory chatMemory) {
        return new Builder(chatMemory);
    }

    @NonNull
    @Override
    public AdvisedResponse aroundCall(@NonNull AdvisedRequest advisedRequest, @NonNull CallAroundAdvisorChain chain) {

        advisedRequest = this.before(advisedRequest);

        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

        this.observeAfter(advisedResponse);

        return advisedResponse;
    }

    @NonNull
    @Override
    public Flux<AdvisedResponse> aroundStream(@NonNull AdvisedRequest advisedRequest,
            @NonNull StreamAroundAdvisorChain chain) {

        Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain,
                this::before);

        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }

    private AdvisedRequest before(AdvisedRequest request) {

        String conversationId = this.doGetConversationId(request.adviseContext());

        int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(request.adviseContext());

        // 1. Retrieve the chat memory for the current conversation.
        List<Message> memoryMessages = this.getChatMemoryStore().get(conversationId, chatMemoryRetrieveSize);

        // 2. Advise the request messages list.
        List<Message> advisedMessages = new ArrayList<>(request.messages());
        advisedMessages.addAll(memoryMessages);

        // 3. Purge the excessive content.
        List<Message> purgedMessages = this.excessiveContentTruncator.truncate(advisedMessages);
        advisedMessages = purgedMessages;

        // 4. Create a new request with the advised messages.
        AdvisedRequest advisedRequest = AdvisedRequest.from(request).messages(advisedMessages).build();

        // 5. Add the new user input to the conversation memory.
        UserMessage userMessage = new UserMessage(request.userText(), request.media());
        this.getChatMemoryStore().add(this.doGetConversationId(request.adviseContext()), userMessage);

        return advisedRequest;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {

        List<Message> assistantMessages = Optional.ofNullable(advisedResponse.response())
                .map(response -> response.getResults()
                .stream()
                .map(g -> (Message) g.getOutput())
                .toList())
                .orElse(List.of());

        this.getChatMemoryStore().add(this.doGetConversationId(advisedResponse.adviseContext()), assistantMessages);
    }

    public static class Builder extends AbstractChatMemoryAdvisor.AbstractBuilder<ChatMemory> {

        protected Builder(ChatMemory chatMemory) {
            super(chatMemory);
        }

        @NonNull
        @Override
        public MessageChatMemoryAdvisor build() {
            return new MessageChatMemoryAdvisor(this.chatMemory, this.conversationId, this.chatMemoryRetrieveSize,
                    this.order);
        }

    }

}
