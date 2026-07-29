package com.bp20.backend.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTests {

    @Test
    void schedulingIsEnabledWithoutMockProfile() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles("local");
            context.register(SchedulingConfig.class);
            context.refresh();

            assertThat(context.containsBean(
                    TaskManagementConfigUtils
                            .SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME
            )).isTrue();
        }
    }
}
