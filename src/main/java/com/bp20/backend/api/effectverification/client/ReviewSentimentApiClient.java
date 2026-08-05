package com.bp20.backend.api.effectverification.client;

import com.bp20.backend.api.effectverification.dto.request.ReviewSentimentRequest;
import com.bp20.backend.api.effectverification.dto.response.ReviewSentimentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class ReviewSentimentApiClient {

    private final RestClient restClient;

    public ReviewSentimentApiClient(
            RestClient.Builder externalRestClientBuilder,
            @Value("${ai.review-sentiment.base-url}") String baseUrl
    ) {
        this.restClient = externalRestClientBuilder.clone()
                .baseUrl(baseUrl)
                .build();
    }

    public ReviewSentimentResponse analyze(Long reviewId, String reviewText) {
        try {
            List<ReviewSentimentResponse> responses = restClient.post()
                    .uri("/api/v1/review/analyze")
                    .body(List.of(new ReviewSentimentRequest(reviewId, reviewText)))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (responses == null
                    || responses.isEmpty()
                    || responses.getFirst().results() == null) {
                throw new IllegalStateException(
                        "Review sentiment AI returned an empty response"
                );
            }
            return responses.getFirst();
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Failed to call review sentiment AI",
                    exception
            );
        }
    }
}
