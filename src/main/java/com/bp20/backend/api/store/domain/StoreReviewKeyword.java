package com.bp20.backend.api.store.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "store_review_keywords")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReviewKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_keyword_id")
    private Long id;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false, length = 50)
    private String aspect;

    @Column(nullable = false, length = 20)
    private String sentiment;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false)
    private Integer count;

    @Column(nullable = false)
    private LocalDateTime analyzedAt;
}
