package com.bp20.backend.api.effectverification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class EffectVerificationTimeConfig {

    @Bean
    public Clock effectVerificationClock() {
        return Clock.systemDefaultZone();
    }
}
