package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.model.Content;
import org.springframework.ai.tokenizer.TokenCountEstimator;

/**
 * Returns a new list of content (e.g list of messages of list of documents)
 * that is a subset of the input list of contents and complies with the max
 * token size constraint. The token estimator is used to estimate the token
 * count of the datum.
 */
public class MaxTokenSizeContenTruncator<T extends Content> implements ExcessiveContentTruncator<T> {

    protected final TokenCountEstimator tokenCountEstimator;

    protected final int maxTokenSize;

    public MaxTokenSizeContenTruncator(TokenCountEstimator tokenCountEstimator, int maxTokenSize) {
        this.tokenCountEstimator = tokenCountEstimator;
        this.maxTokenSize = maxTokenSize;
    }

    @Override
    public List<T> truncate(List<T> datum) {

        int index = 0;
        List<T> newList = new ArrayList<>();
        int totalSize = this.doEstimateTokenCount(datum);

        while (index < datum.size() && totalSize > this.maxTokenSize) {
            T oldDatum = datum.get(index++);
            int oldMessageTokenSize = this.doEstimateTokenCount(oldDatum);
            totalSize = totalSize - oldMessageTokenSize;
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
