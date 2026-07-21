package com.bp20.backend.effectverification.client;

import com.bp20.backend.effectverification.dto.request.ReviewSentimentRequest;
import com.bp20.backend.effectverification.dto.response.ReviewSentimentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ReviewSentimentApiClient {

    private final RestClient restClient;

    public ReviewSentimentApiClient(
            @Value("${ai.review-sentiment.base-url}") String baseUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public ReviewSentimentResponse analyze(String reviewText) {
        try {
            ReviewSentimentResponse response = restClient.post()
                    .uri("/api/v1/review/predict")
                    .body(new ReviewSentimentRequest(reviewText))
                    .retrieve()
                    .body(ReviewSentimentResponse.class);

            if (response == null || response.results() == null) {
                throw new IllegalStateException(
                        "Review sentiment AI returned an empty response"
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Failed to call review sentiment AI",
                    exception
            );
        }
    }
}

