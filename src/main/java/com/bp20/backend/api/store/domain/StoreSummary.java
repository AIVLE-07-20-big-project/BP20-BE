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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    private Integer totalReviewsAnalyzed;

    private LocalDateTime analyzedAt;

    @Builder
    public StoreSummary(Store store, String summary, Integer totalReviewsAnalyzed, LocalDateTime analyzedAt) {
        this.store = store;
        this.summary = summary;
        this.totalReviewsAnalyzed = totalReviewsAnalyzed;
        this.analyzedAt = analyzedAt != null ? analyzedAt : LocalDateTime.now();
    }
}
