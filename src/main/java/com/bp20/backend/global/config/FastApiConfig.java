package com.bp20.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FastApiConfig {

    @Bean
    RestClient fastApiRestClient(
            @Qualifier("externalRestClientBuilder")
            RestClient.Builder builder,
            @Value("${app.fastapi.base-url}") String baseUrl
    ) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
    }
}
