package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.model.Content;
import org.springframework.ai.tokenizer.TokenCountEstimator;

import lombok.extern.slf4j.Slf4j;

/**
 * Returns a new list of content (e.g list of messages of list of documents)
 * that is a subset of the input list of contents and complies with the max
 * token size constraint. The token estimator is used to estimate the token
 * count of the datum.
 */
@Slf4j
public class MaxTokenSizeContentLimiter<T extends Content> implements ContextLimiter<T> {

    protected final TokenCountEstimator tokenCountEstimator;

    protected final int maxTokenSize;

    public MaxTokenSizeContentLimiter(TokenCountEstimator tokenCountEstimator, int maxTokenSize) {
        this.tokenCountEstimator = tokenCountEstimator;
        this.maxTokenSize = maxTokenSize;
    }

    @Override
    public List<T> truncate(List<T> datum) {

        int index = 0;
        List<T> newList = new ArrayList<>();
        int totalSize = this.doEstimateTokenCount(datum);
        log.debug("total estimated token size: {}", totalSize);

        while (index < datum.size() && totalSize > this.maxTokenSize) {
            T oldDatum = datum.get(index++);
            int oldMessageTokenSize = this.doEstimateTokenCount(oldDatum);
            log.debug("old message token size: {}", oldMessageTokenSize);
            totalSize = totalSize - oldMessageTokenSize;
            log.debug("total size after truncation: {}", totalSize);
        }

        if (index >= datum.size()) {
            return List.of();
        }

        // add the rest of the messages.
        newList.addAll(datum.subList(index, datum.size()));

        return newList;
    }

    protected int doEstimateTokenCount(T datum) {
        return this.tokenCountEstimator.estimate(datum.getText());
    }

    protected int doEstimateTokenCount(List<T> datum) {
        return datum.stream().mapToInt(this::doEstimateTokenCount).sum();
    }

}
