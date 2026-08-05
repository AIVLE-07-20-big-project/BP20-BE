package com.bp20.backend.api.store.dto.response;

import com.bp20.backend.api.store.domain.Store;

/**
 * 내부(AI 서버) 전용 응답 — 신규 가맹점 영업 타겟 추천 배치가 쓰는 세 가지 용도를 겸한다.
 *   1) "이미 자사 가맹점인 업장" 걸러내기 (businessNumber/address 매칭)
 *   2) "우수 가맹점(Hero Store)" 선정용 근사 지표 (salesGrowthRate)
 *   3) 우수 가맹점 선정용 리뷰 안정성 지표 (reviewCount/reviewAvgRating/reviewRatingStd)
 *
 * salesGrowthRate는 최근 3개월 매출 합계와 이전 3개월 매출 합계를 비교한 성장률이다.
 * 비교 기준이 될 이전 3개월 매출 데이터가 없으면(신규 매장 등) null을 내려준다.
 *
 * reviewRatingStd(평점 표준편차)는 리뷰가 3개 미만이면 null을 내려준다 — 리뷰 1~2개로 "평점이
 * 안정적이다/아니다"를 판단하는 건 근거가 너무 약하다고 판단했다. reviewAvgRating은 리뷰가
 * 1개라도 있으면 값을 내려준다(표준편차와 달리 평균은 표본이 적어도 의미가 아예 없진 않다).
 *
 * 점주용 StoreResponse와 달리 id, phoneNumber, onlineSalesStatus, createdAt/updatedAt 등은
 * 의도적으로 제외한다(외부 서비스 경계로 불필요한 내부 필드를 넘기지 않는다).
 */
public record StoreRegistryEntryResponse(
        String businessNumber,
        String name,
        String category,
        String address,
        Double salesGrowthRate,
        Integer reviewCount,
        Double reviewAvgRating,
        Double reviewRatingStd
) {
    public static StoreRegistryEntryResponse of(
            Store store,
            Double salesGrowthRate,
            Integer reviewCount,
            Double reviewAvgRating,
            Double reviewRatingStd
    ) {
        return new StoreRegistryEntryResponse(
                store.getBusinessNumber(),
                store.getName(),
                store.getCategory(),
                store.getAddress(),
                salesGrowthRate,
                reviewCount,
                reviewAvgRating,
                reviewRatingStd
        );
    }
}
