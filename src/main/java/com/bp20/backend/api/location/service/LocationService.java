package com.bp20.backend.api.location.service;

import com.bp20.backend.api.location.client.KakaoLocalClient;
import com.bp20.backend.api.location.domain.StoreOwnerLocation;
import com.bp20.backend.api.location.dto.LocationSearchResponse;
import com.bp20.backend.api.location.dto.SaveLocationRequest;
import com.bp20.backend.api.location.repository.StoreOwnerLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LocationService {
    private final KakaoLocalClient kakaoLocalClient;
    private final StoreOwnerLocationRepository locationRepository;

    public List<LocationSearchResponse> search(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("위치는 두 글자 이상 입력해 주세요.");
        }
        return kakaoLocalClient.search(normalized);
    }

    @Transactional(readOnly = true)
    public Optional<LocationSearchResponse> getSavedLocation(Long ownerId) {
        return locationRepository.findByOwnerId(ownerId)
                .map(location -> new LocationSearchResponse(
                        location.getDisplayName(),
                        location.getLatitude(),
                        location.getLongitude()
                ));
    }

    @Transactional
    public LocationSearchResponse saveLocation(
            Long ownerId,
            SaveLocationRequest request
    ) {
        String displayName = request.displayName().trim();
        StoreOwnerLocation location = locationRepository.findByOwnerId(ownerId)
                .orElseGet(() -> StoreOwnerLocation.create(
                        ownerId,
                        displayName,
                        request.latitude(),
                        request.longitude()
                ));

        location.update(
                displayName,
                request.latitude(),
                request.longitude()
        );
        StoreOwnerLocation saved = locationRepository.save(location);

        return new LocationSearchResponse(
                saved.getDisplayName(),
                saved.getLatitude(),
                saved.getLongitude()
        );
    }
}
