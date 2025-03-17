package com.moguyn.deepdesk.advisor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.stereotype.Service;

import com.moguyn.deepdesk.config.CoreSettings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of AdvisorService that configures advisors based on
 * application settings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAdvisorService implements AdvisorService {

    private final ChatMemoryAdvisor chatMemoryAdvisor;

    @Override
    public List<Advisor> getEnabledAdvisors(CoreSettings settings) {
        List<Advisor> enabledAdvisors = new ArrayList<>();

        CoreSettings.Advisors advisorSettings = settings.advisors();
        if (advisorSettings != null) {
            if (advisorSettings.isChatMemoryAdvisorEnabled()) {
                log.info("Enabling Chat Memory Advisor");
                enabledAdvisors.add(chatMemoryAdvisor);
            }
        } else {
            log.warn("No advisor configuration found, enabling all advisors by default");
            enabledAdvisors.add(chatMemoryAdvisor);
        }

        return enabledAdvisors;
    }
}
