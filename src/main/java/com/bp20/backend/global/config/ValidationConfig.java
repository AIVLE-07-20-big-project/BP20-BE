package com.bp20.backend.global.config;

import jakarta.validation.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ValidationConfig {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public LocalValidatorFactoryBean defaultValidator() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setConfigurationInitializer(configuration ->
                configuration.clockProvider(koreaClockProvider())
        );
        return validator;
    }

    private ClockProvider koreaClockProvider() {
        return () -> Clock.system(KOREA_ZONE);
    }
}
