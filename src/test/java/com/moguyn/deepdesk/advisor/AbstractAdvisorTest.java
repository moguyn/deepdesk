package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class AbstractAdvisorTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CallAroundAdvisorChain chain;

    @Mock
    private StreamAroundAdvisorChain streamChain;

    @Mock
    private ChatModel chatModel;

    private TestAdvisor advisor;

    @BeforeEach
    void setUp() {
        advisor = new TestAdvisor(objectMapper, 100);
    }

    @Test
    void testAroundCall() {
        // Arrange
        AdvisedRequest request = AdvisedRequest.builder()
                .userText("test query")
                .chatModel(chatModel)
                .build();

        Map<String, Object> adviseContext = new HashMap<>();
        adviseContext.put("key", "value");

        AdvisedResponse expectedResponse = AdvisedResponse.builder()
                .adviseContext(adviseContext)
                .build();

        when(chain.nextAroundCall(any(AdvisedRequest.class))).thenReturn(expectedResponse);

        // Act
        AdvisedResponse response = advisor.aroundCall(request, chain);

        // Assert
        assertNotNull(response);
        verify(chain).nextAroundCall(request);
    }

    @Test
    void testAroundStream() {
        // Arrange
        AdvisedRequest request = AdvisedRequest.builder()
                .userText("test query")
                .chatModel(chatModel)
                .build();

        Map<String, Object> adviseContext = new HashMap<>();
        adviseContext.put("key", "value");

        AdvisedResponse expectedResponse = AdvisedResponse.builder()
                .adviseContext(adviseContext)
                .build();

        when(streamChain.nextAroundStream(any(AdvisedRequest.class)))
                .thenReturn(Flux.just(expectedResponse));

        // Act
        Flux<AdvisedResponse> responseFlux = advisor.aroundStream(request, streamChain);

        // Assert
        // Using simple assertions instead of StepVerifier
        assertEquals(1, responseFlux.collectList().block().size());
        verify(streamChain).nextAroundStream(request);
    }

    @Test
    void testGetOrder() {
        // Act & Assert
        assertEquals(100, advisor.getOrder());
    }

    @Test
    void testDescribeTool() {
        // Arrange
        FunctionCallback functionCallback = mock(FunctionCallback.class);
        when(functionCallback.getName()).thenReturn("testFunction");
        when(functionCallback.getDescription()).thenReturn("Test description");
        when(functionCallback.getInputTypeSchema()).thenReturn("Test schema");

        // Act
        String result = advisor.testDescribeTool(functionCallback);

        // Assert
        assertEquals("testFunction(description: Test description, input schema: Test schema)", result);
    }

    @Test
    void testAppendAdvice() throws JsonProcessingException {
        // Arrange
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage("Initial message"));
        Map<String, String> advice = new HashMap<>();
        advice.put("key", "value");

        when(objectMapper.writeValueAsString(any())).thenReturn("{\"key\":\"value\"}");

        // Act
        List<Message> result = advisor.testAppendAdvice(messages, advice, MessageType.USER);

        // Assert
        assertEquals(2, result.size());
        assertEquals("{\"key\":\"value\"}", ((UserMessage) result.get(1)).getText());
    }

    // Test implementation of AbstractAdvisor for testing
    private static class TestAdvisor extends AbstractAdvisor {

        public TestAdvisor(ObjectMapper objectMapper, int order) {
            super(objectMapper, order);
        }

        @Override
        public String getName() {
            return "test-advisor";
        }

        public String testDescribeTool(FunctionCallback f) {
            return describeTool(f);
        }

        public List<Message> testAppendAdvice(List<Message> messages, Object advice, MessageType messageType) throws JsonProcessingException {
            return appendAdvice(messages, advice, messageType);
        }
    }
}
