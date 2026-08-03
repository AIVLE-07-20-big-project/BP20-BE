package com.bp20.backend.api.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "store_summary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long storeId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    private Integer totalReviewsAnalyzed;

    private LocalDateTime analyzedAt;

    @Builder
    public StoreSummary(Long storeId, String summary, Integer totalReviewsAnalyzed, LocalDateTime analyzedAt) {
        this.storeId = storeId;
        this.summary = summary;
        this.totalReviewsAnalyzed = totalReviewsAnalyzed;
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
    }
}
