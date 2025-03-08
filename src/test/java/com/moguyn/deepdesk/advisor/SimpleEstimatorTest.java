package com.moguyn.deepdesk.advisor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;

public class SimpleEstimatorTest {

    @Test
    void testEstimate() {
        JTokkitTokenCountEstimator estimator = new JTokkitTokenCountEstimator();
        assertEquals(4, estimator.estimate("Hello, world!"));
    }
}
