package com.bp20.backend.api.location.service;

import com.bp20.backend.api.location.client.KakaoLocalClient;
import com.bp20.backend.api.location.dto.LocationSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final KakaoLocalClient kakaoLocalClient;

    public List<LocationSearchResponse> search(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("위치는 두 글자 이상 입력해 주세요.");
        }
        return kakaoLocalClient.search(normalized);
    }
}
