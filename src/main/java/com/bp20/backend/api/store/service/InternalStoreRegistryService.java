package com.bp20.backend.api.store.service;

import com.bp20.backend.api.csv.repository.CsvDailySalesRepository;
import com.bp20.backend.api.review.domain.Review;
import com.bp20.backend.api.review.repository.ReviewRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.dto.response.StoreRegistryEntryResponse;
import com.bp20.backend.api.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 서버(FastAPI)가 "신규 가맹점 영업 타겟 추천" 배치에서 쓰는 조회 전용 서비스.
 *   1) 자사 가맹점 걸러내기용 목록
 *   2) 우수 가맹점(Hero Store) 근사 선정용 매출 성장률 + 리뷰 안정성
 *
 * StoreService는 "점주 본인 매장 1건"을 다루는 책임(등록/조회/수정)만 갖고 있어서,
 * "전체 가맹점을 서비스 간 API로 노출"하는 이 책임을 거기 섞지 않고 별도 서비스로 분리했다.
 */
@Service
@RequiredArgsConstructor
public class InternalStoreRegistryService {

    private static final int TREND_WINDOW_MONTHS = 3;
    // 리뷰 표준편차를 계산할 최소 리뷰 수. 이보다 적으면 "평점이 안정적이다"를 판단할 근거가
    // 부족하다고 보고 표준편차를 null로 내려서 우수 가맹점 후보에서 자동 제외되게 한다.
    private static final int MIN_REVIEWS_FOR_STD = 3;

    private final StoreRepository storeRepository;
    private final CsvDailySalesRepository csvDailySalesRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<StoreRegistryEntryResponse> getAllForRegistry() {
        LocalDate today = LocalDate.now();
        LocalDate recentPeriodStart = today.minusMonths(TREND_WINDOW_MONTHS);
        LocalDate previousPeriodStart = today.minusMonths((long) TREND_WINDOW_MONTHS * 2);

        return storeRepository.findAll().stream()
                .map(store -> {
                    Double salesGrowthRate = computeSalesGrowthRate(
                            store, previousPeriodStart, recentPeriodStart, today
                    );
                    ReviewStats reviewStats = computeReviewStats(store.getId());
                    return StoreRegistryEntryResponse.of(
                            store,
                            salesGrowthRate,
                            reviewStats.count(),
                            reviewStats.avgRating(),
                            reviewStats.stdDev()
                    );
                })
                .toList();
    }

    /**
     * (최근 3개월 매출 합계 - 이전 3개월 매출 합계) / 이전 3개월 매출 합계.
     * 이전 3개월 매출 합계가 0이면(데이터 없음) null을 반환한다 — 0으로 나누기를 피하는 동시에
     * "성장률 0%"와 명확히 구분하기 위함이다.
     */
    private Double computeSalesGrowthRate(
            Store store,
            LocalDate previousPeriodStart,
            LocalDate recentPeriodStart,
            LocalDate today
    ) {
        Long ownerId = store.getOwner().getId();

        long previousSum = csvDailySalesRepository.sumSalesAmountBetween(
                ownerId, previousPeriodStart, recentPeriodStart
        );
        long recentSum = csvDailySalesRepository.sumSalesAmountBetween(
                ownerId, recentPeriodStart, today
        );

        if (previousSum == 0) {
            return null;
        }
        return (recentSum - previousSum) / (double) previousSum;
    }

    /**
     * 매장의 리뷰 개수/평균 평점/평점 표준편차(모집단 기준)를 계산한다.
     * DB의 stddev 집계 함수를 쓰지 않고 자바에서 직접 계산하는 이유: MySQL(운영)과 H2(테스트)의
     * stddev 함수 지원/이름이 서로 달라 테스트-운영 간 결과가 어긋날 위험이 있다. 리뷰 수가
     * 매장당 많지 않을 것으로 예상되는 초기 단계라 자바에서 계산해도 성능 문제는 없다.
     */
    private ReviewStats computeReviewStats(Long storeId) {
        List<Review> reviews = reviewRepository.findByStoreId(storeId);
        if (reviews.isEmpty()) {
            return new ReviewStats(0, null, null);
        }

        double sum = 0.0;
        for (Review review : reviews) {
            sum += review.getRating();
        }
        double mean = sum / reviews.size();

        Double stdDev = null;
        if (reviews.size() >= MIN_REVIEWS_FOR_STD) {
            double squaredDiffSum = 0.0;
            for (Review review : reviews) {
                double diff = review.getRating() - mean;
                squaredDiffSum += diff * diff;
            }
            stdDev = Math.sqrt(squaredDiffSum / reviews.size());
        }

        return new ReviewStats(reviews.size(), mean, stdDev);
    }

    private record ReviewStats(int count, Double avgRating, Double stdDev) {
    }
}
