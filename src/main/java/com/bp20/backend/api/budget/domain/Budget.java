package com.bp20.backend.api.budget.domain;

import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리별 월 예산 목표치. (2번 AI 가계부의 "예산 초과 확인" 기능에 필요한 최소 엔티티)
 * yearMonth는 "YYYY-MM" 형식 문자열로 저장한다 (Python 분석 서비스와 포맷을 맞추기 위함).
 */
@Getter
@Entity
@Table(name = "budgets", indexes = {
        @Index(name = "idx_budgets_store_month", columnList = "store_id, budget_month")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "budget_month", nullable = false, length = 7)
    private String yearMonth; // "2026-06"

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false)
    private Integer budgetAmount;

    private Budget(Long storeId, String yearMonth, String category, Integer budgetAmount) {
        this.storeId = storeId;
        this.yearMonth = yearMonth;
        this.category = category;
        this.budgetAmount = budgetAmount;
    }

    public static Budget create(Long storeId, String yearMonth, String category, Integer budgetAmount) {
        return new Budget(storeId, yearMonth, category, budgetAmount);
    }

    public void updateAmount(Integer budgetAmount) {
        this.budgetAmount = budgetAmount;
    }
}