package com.moguyn.deepdesk.advisor;

import java.util.List;

import org.springframework.ai.model.Content;

public interface ContextLimiter<T extends Content> {

    List<T> truncate(List<T> messages);
}
