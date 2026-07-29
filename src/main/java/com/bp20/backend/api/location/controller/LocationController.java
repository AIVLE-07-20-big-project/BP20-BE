package com.bp20.backend.api.location.controller;

import com.bp20.backend.api.location.dto.LocationSearchResponse;
import com.bp20.backend.api.location.dto.SaveLocationRequest;
import com.bp20.backend.api.location.service.LocationService;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/locations")
public class LocationController {
    private final LocationService locationService;

    @GetMapping("/search")
    public List<LocationSearchResponse> search(@RequestParam String query) {
        return locationService.search(query);
    }

    @GetMapping("/saved")
    public ResponseEntity<LocationSearchResponse> getSavedLocation(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return locationService.getSavedLocation(currentUser.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping("/saved")
    public LocationSearchResponse saveLocation(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody SaveLocationRequest request
    ) {
        return locationService.saveLocation(currentUser.id(), request);
    }
}
