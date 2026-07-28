package com.bp20.backend.api.location.controller;

import com.bp20.backend.api.location.dto.LocationSearchResponse;
import com.bp20.backend.api.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
