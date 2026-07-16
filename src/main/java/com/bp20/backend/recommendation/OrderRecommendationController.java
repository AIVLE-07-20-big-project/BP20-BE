package com.bp20.backend.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-recommendations")
public class OrderRecommendationController {

    private final OrderRecommendationService service;

    @PostMapping("/generate")
    public List<OrderRecommendationResponse> generate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate referenceDate
    ) {
        LocalDate targetDate =
                referenceDate == null
                        ? LocalDate.now()
                        : referenceDate;

        return service.generate(targetDate);
    }
}