package com.bp20.backend.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OcrServiceProperties.class)
public class OcrServiceClientConfig {

    @Bean
    public RestClient ocrServiceRestClient(OcrServiceProperties properties) {
        // JDK 기본 HttpClient는 멀티파트(파일 업로드) 요청에서 불안정한 문제가 있어
        // Apache HttpClient5로 명시적으로 교체한다.
        //
        // 타임아웃도 넉넉하게 잡는다 - PaddleOCR 처리 자체가 CPU 환경에서
        // 이미지 1장당 수십 초 걸릴 수 있어서, 기본 타임아웃(짧음)으로는 요청이
        // 끝나기 전에 Java가 먼저 포기(Read timed out)해버리는 문제가 있었다.
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(180))  // OCR 처리 시간 감안 (3분)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}