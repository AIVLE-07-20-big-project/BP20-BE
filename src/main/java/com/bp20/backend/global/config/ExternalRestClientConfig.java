package com.bp20.backend.global.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 API(주로 팀 내 FastAPI 서비스들)를 호출할 때 공통으로 쓸 수 있는 RestClient.Builder.
 *
 * JDK 기본 HttpClient는 멀티파트(파일 업로드) 요청에서 불안정한 문제가 있어
 * Apache HttpClient5로 명시적으로 교체했고, 타임아웃도 넉넉하게 잡아뒀다
 * (OCR처럼 CPU 연산이 오래 걸리는 요청도 버틸 수 있도록 응답 타임아웃 180초).
 *
 * 사용 예 - 각자의 도메인 서비스에서 baseUrl만 지정해서 clone 후 사용:
 *
 *     private final RestClient myServiceClient;
 *
 *     public MyServiceClient(RestClient.Builder externalRestClientBuilder, MyServiceProperties props) {
 *         this.myServiceClient = externalRestClientBuilder.clone()
 *                 .baseUrl(props.baseUrl())
 *                 .build();
 *     }
 */
@Configuration
public class ExternalRestClientConfig {

    @Bean
    public RestClient.Builder externalRestClientBuilder() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setResponseTimeout(Timeout.ofSeconds(180))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    @Bean
    public RestClient restClient(RestClient.Builder externalRestClientBuilder) {
        return externalRestClientBuilder.build();
    }
}