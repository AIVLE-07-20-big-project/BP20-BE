package com.bp20.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile("mock")
@EnableScheduling
public class MockSchedulingConfig {
}
