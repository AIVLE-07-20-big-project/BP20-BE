package com.bp20.backend.api.review.domain;

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
    private Long id;

    private Long storeId;

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
