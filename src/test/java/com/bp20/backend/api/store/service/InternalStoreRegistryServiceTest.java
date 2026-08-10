package com.bp20.backend.api.store.service;

import com.bp20.backend.api.csv.repository.CsvDailySalesRepository;
import com.bp20.backend.api.review.domain.Review;
import com.bp20.backend.api.review.repository.ReviewRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.dto.response.StoreRegistryEntryResponse;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalStoreRegistryServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private CsvDailySalesRepository csvDailySalesRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private InternalStoreRegistryService service;

    private Store mockStore(Long storeId, Long ownerId) {
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(store.getBusinessNumber()).thenReturn("111-11-11111");
        when(store.getName()).thenReturn("테스트카페");
        when(store.getCategory()).thenReturn("카페");
        when(store.getAddress()).thenReturn("서울 강남구 테스트로 1");

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(ownerId);
        when(store.getOwner()).thenReturn(owner);

        return store;
    }

    private Review reviewWithRating(double rating) {
        Review review = mock(Review.class);
        when(review.getRating()).thenReturn(rating);
        return review;
    }

    @Test
    void 필드가_올바르게_매핑된다() {
        Store store = mockStore(1L, 10L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(csvDailySalesRepository.sumSalesAmountBetween(eq(10L), any(), any())).thenReturn(0L);
        when(reviewRepository.findByStore_Id(1L)).thenReturn(List.of());

        List<StoreRegistryEntryResponse> result = service.getAllForRegistry();

        assertThat(result).hasSize(1);
        StoreRegistryEntryResponse response = result.get(0);
        assertThat(response.businessNumber()).isEqualTo("111-11-11111");
        assertThat(response.name()).isEqualTo("테스트카페");
        assertThat(response.category()).isEqualTo("카페");
        assertThat(response.address()).isEqualTo("서울 강남구 테스트로 1");
    }

    @Test
    void 매출_성장률이_50퍼센트_증가로_계산된다() {
        Store store = mockStore(1L, 10L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        LocalDate today = LocalDate.now();
        LocalDate recentStart = today.minusMonths(3);
        LocalDate previousStart = today.minusMonths(6);
        when(csvDailySalesRepository.sumSalesAmountBetween(10L, previousStart, recentStart)).thenReturn(1_000_000L);
        when(csvDailySalesRepository.sumSalesAmountBetween(10L, recentStart, today)).thenReturn(1_500_000L);
        when(reviewRepository.findByStore_Id(1L)).thenReturn(List.of());

        List<StoreRegistryEntryResponse> result = service.getAllForRegistry();

        assertThat(result.get(0).salesGrowthRate()).isEqualTo(0.5);
    }

    @Test
    void 이전_3개월_매출이_없으면_성장률은_null이다() {
        Store store = mockStore(1L, 10L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(csvDailySalesRepository.sumSalesAmountBetween(any(), any(), any())).thenReturn(0L);
        when(reviewRepository.findByStore_Id(1L)).thenReturn(List.of());

        List<StoreRegistryEntryResponse> result = service.getAllForRegistry();

        assertThat(result.get(0).salesGrowthRate()).isNull();
    }

    @Test
    void 리뷰가_없으면_카운트는_0이고_평균평점과_표준편차는_null이다() {
        Store store = mockStore(1L, 10L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(csvDailySalesRepository.sumSalesAmountBetween(any(), any(), any())).thenReturn(0L);
        when(reviewRepository.findByStore_Id(1L)).thenReturn(List.of());

        StoreRegistryEntryResponse response = service.getAllForRegistry().get(0);

        assertThat(response.reviewCount()).isEqualTo(0);
        assertThat(response.reviewAvgRating()).isNull();
        assertThat(response.reviewRatingStd()).isNull();
    }

    @Test
    void 리뷰가_3개_미만이면_평균은_계산되지만_표준편차는_null이다() {
        Store store = mockStore(1L, 10L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(csvDailySalesRepository.sumSalesAmountBetween(any(), any(), any())).thenReturn(0L);

        List<Review> reviews = List.of(reviewWithRating(5.0), reviewWithRating(4.0));
        when(reviewRepository.findByStore_Id(1L)).thenReturn(reviews);

        StoreRegistryEntryResponse response = service.getAllForRegistry().get(0);

        assertThat(response.reviewCount()).isEqualTo(2);
        assertThat(response.reviewAvgRating()).isEqualTo(4.5);
        assertThat(response.reviewRatingStd()).isNull();
    }

    @Test
    void 리뷰가_3개_이상이면_평균과_표준편차가_모두_계산된다() {
        Store store = mockStore(1L, 10L);
        when(storeRepository.findAll()).thenReturn(List.of(store));
        when(csvDailySalesRepository.sumSalesAmountBetween(any(), any(), any())).thenReturn(0L);

        // 평점 4,4,4,4 -> 평균 4.0, 표준편차 0.0 (완전히 안정적인 케이스)
        List<Review> reviews = List.of(
                reviewWithRating(4.0), reviewWithRating(4.0), reviewWithRating(4.0), reviewWithRating(4.0)
        );
        when(reviewRepository.findByStore_Id(1L)).thenReturn(reviews);

        StoreRegistryEntryResponse response = service.getAllForRegistry().get(0);

        assertThat(response.reviewCount()).isEqualTo(4);
        assertThat(response.reviewAvgRating()).isEqualTo(4.0);
        assertThat(response.reviewRatingStd()).isEqualTo(0.0);
    }
}
