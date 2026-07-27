package com.bp20.backend.location.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.bp20.backend.location.dto.LocationSearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class KakaoLocalClient {

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalClient(
            @Qualifier("restClientBuilder") RestClient.Builder builder,
            @Value("${kakao.local.base-url:https://dapi.kakao.com}") String baseUrl,
            @Value("${kakao.local.rest-api-key:}") String restApiKey
    ) {
        this.restClient = builder.clone().baseUrl(baseUrl).build();
        this.restApiKey = restApiKey;
    }

    public List<LocationSearchResponse> search(String query) {
        if (restApiKey == null || restApiKey.isBlank()) {
            throw new IllegalStateException("KAKAO_REST_API_KEY가 설정되지 않았습니다.");
        }

        try {
            KakaoAddressResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/address.json")
                            .queryParam("query", query)
                            .queryParam("size", 10)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(KakaoAddressResponse.class);

            if (response == null || response.documents() == null) {
                return List.of();
            }

            Map<String, LocationSearchResponse> unique = new LinkedHashMap<>();
            for (KakaoAddressDocument document : response.documents()) {
                LocationSearchResponse location = toLocation(document);
                if (location != null) {
                    unique.putIfAbsent(location.displayName(), location);
                }
            }
            return unique.values().stream().limit(5).toList();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "카카오 위치 검색에 실패했습니다. API 키와 카카오 로컬 API 사용 설정을 확인해 주세요.",
                    exception
            );
        }
    }

    private LocationSearchResponse toLocation(KakaoAddressDocument document) {
        try {
            double longitude = Double.parseDouble(document.x());
            double latitude = Double.parseDouble(document.y());
            String displayName = document.address() == null
                    ? document.addressName()
                    : joinRegion(document.address());
            if (displayName == null || displayName.isBlank()) {
                return null;
            }
            return new LocationSearchResponse(displayName, latitude, longitude);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String joinRegion(KakaoAddress address) {
        return Stream.of(
                        address.region1DepthName(),
                        address.region2DepthName(),
                        address.region3DepthName()
                )
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse(address.addressName());
    }

    private record KakaoAddressResponse(List<KakaoAddressDocument> documents) {
    }

    private record KakaoAddressDocument(
            @JsonProperty("address_name") String addressName,
            String x,
            String y,
            KakaoAddress address
    ) {
    }

    private record KakaoAddress(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("region_1depth_name") String region1DepthName,
            @JsonProperty("region_2depth_name") String region2DepthName,
            @JsonProperty("region_3depth_name") String region3DepthName
    ) {
    }
}
