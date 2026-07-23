package com.bp20.backend.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * ForecastClient, LlmClient 등에서 공통으로 사용할
     * RestClient.Builder를 Spring Bean으로 등록한다.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}