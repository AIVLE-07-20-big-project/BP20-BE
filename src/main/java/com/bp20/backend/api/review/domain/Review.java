package com.bp20.backend.api.review.domain;

import com.bp20.backend.api.store.domain.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime reviewedDate;

    @Builder.Default
    @Column(nullable = false)
    private boolean isAnalyzed = false;

    public void markAsAnalyzed() {
        this.isAnalyzed = true;
    }
}
