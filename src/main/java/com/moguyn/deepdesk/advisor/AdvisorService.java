package com.moguyn.deepdesk.advisor;

import java.util.List;

import org.springframework.ai.chat.client.advisor.api.Advisor;

import com.moguyn.deepdesk.config.CoreSettings;

/**
 * Service responsible for determining which advisors should be enabled based on
 * application configuration.
 */
public interface AdvisorService {

    /**
     * Returns a list of enabled advisors based on application settings.
     *
     * @param settings Application core settings
     * @return List of enabled advisor instances
     */
    List<Advisor> getEnabledAdvisors(CoreSettings settings);
}
