package com.bp20.backend.api.effectverification.service;

import com.bp20.backend.api.ai.domain.AiRecommendationRun;
import com.bp20.backend.api.ai.repository.AiRecommendationRunRepository;
import com.bp20.backend.api.effectverification.dto.request.*;
import com.bp20.backend.api.effectverification.dto.response.EffectVerificationResponse;
import com.bp20.backend.api.effectverification.dto.response.VerificationExecutionResponse;
import com.bp20.backend.api.effectverification.domain.EffectVerificationExecution;
import com.bp20.backend.api.effectverification.domain.VerificationStatus;
import com.bp20.backend.api.effectverification.repository.EffectVerificationExecutionRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectVerificationLifecycleServiceTests {

    private static final long USER_ID = 7L;

    @Mock
    private EffectVerificationExecutionRepository executionRepository;

    @Mock
    private EffectVerificationService verificationService;

    @Mock
    private AiRecommendationRunRepository recommendationRunRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private User user;

    @Mock
    private Store store;

    @Mock
    private AiRecommendationRun recommendationRun;

    private EffectVerificationLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        lifecycleService = new EffectVerificationLifecycleService(
                executionRepository,
                verificationService,
                recommendationRunRepository,
                userRepository,
                storeRepository
        );
        lenient().when(user.getId()).thenReturn(USER_ID);
        lenient().when(store.getId()).thenReturn(1L);
        lenient().when(recommendationRun.getThreadId())
                .thenReturn("11111111-1111-1111-1111-111111111111");
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        lenient().when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        lenient().when(recommendationRunRepository.findByThreadIdAndUser_Id(
                "11111111-1111-1111-1111-111111111111",
                USER_ID
        )).thenReturn(Optional.of(recommendationRun));
    }

    @Test
    void registerExecutionStoresBaselineAndFourteenDayDueDate() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        ExecutionRegistrationRequest request = registrationRequest(executedAt);
        request.setThreadId("11111111-1111-1111-1111-111111111111");
        request.setDecisionId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(executionRepository.existsByAiRecommendationId("100")).thenReturn(false);
        when(executionRepository.existsByRecommendationRun_ThreadId(request.getThreadId())).thenReturn(false);
        when(executionRepository.existsByDecisionId(request.getDecisionId())).thenReturn(false);
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationExecutionResponse response =
                lifecycleService.registerExecution(USER_ID, request);

        assertThat(response.getStatus()).isEqualTo(VerificationStatus.COLLECTING);
        assertThat(response.getThreadId()).isEqualTo(request.getThreadId());
        assertThat(response.getDecisionId()).isEqualTo(request.getDecisionId());
        assertThat(response.getVerificationDueAt()).isEqualTo(executedAt.plusDays(14));
        ArgumentCaptor<EffectVerificationExecution> captor =
                ArgumentCaptor.forClass(EffectVerificationExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getRecommendationRun().getThreadId())
                .isEqualTo(request.getThreadId());
        assertThat(captor.getValue().getDecisionId()).isEqualTo(request.getDecisionId());
        assertThat(captor.getValue().getBeforeMetricsJson()).contains("target_sales");
    }

    @Test
    void registerExecutionUsesThreadIdWhenRecommendationIdIsMissing() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        String threadId = "11111111-1111-1111-1111-111111111111";
        ExecutionRegistrationRequest request = registrationRequest(executedAt);
        request.setRecommendationId(null);
        request.setThreadId(threadId);
        when(executionRepository.existsByAiRecommendationId(threadId)).thenReturn(false);
        when(executionRepository.existsByRecommendationRun_ThreadId(threadId)).thenReturn(false);
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VerificationExecutionResponse response =
                lifecycleService.registerExecution(USER_ID, request);

        assertThat(response.getRecommendationId()).isEqualTo(threadId);
        assertThat(response.getThreadId()).isEqualTo(threadId);
    }

    @Test
    void linkCampaignDecisionUpdatesExecutionByThreadId() {
        String threadId = "11111111-1111-1111-1111-111111111111";
        String decisionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        EffectVerificationExecution execution = EffectVerificationExecution.builder()
                .aiRecommendationId(threadId)
                .recommendationRun(recommendationRun)
                .user(user)
                .store(store)
                .recommendationType(RecommendationType.SALES)
                .status(VerificationStatus.COLLECTING)
                .conditionJson("{}")
                .beforeMetricsJson("{}")
                .executedAt(LocalDateTime.now())
                .verificationDueAt(LocalDateTime.now().plusDays(14))
                .build();
        when(executionRepository.findByRecommendationRun_ThreadIdAndUser_Id(threadId, USER_ID))
                .thenReturn(Optional.of(execution));
        when(executionRepository.existsByDecisionId(decisionId)).thenReturn(false);
        when(executionRepository.save(execution)).thenReturn(execution);

        VerificationExecutionResponse response = lifecycleService.linkCampaignDecision(
                USER_ID,
                threadId,
                decisionId
        );

        assertThat(response.getDecisionId()).isEqualTo(decisionId);
        verify(executionRepository).save(execution);
    }

    @Test
    void linkCampaignDecisionIsIdempotentForSameDecision() {
        String threadId = "11111111-1111-1111-1111-111111111111";
        String decisionId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
        EffectVerificationExecution execution = EffectVerificationExecution.builder()
                .aiRecommendationId(threadId)
                .recommendationRun(recommendationRun)
                .decisionId(decisionId)
                .user(user)
                .store(store)
                .recommendationType(RecommendationType.SALES)
                .status(VerificationStatus.COLLECTING)
                .conditionJson("{}")
                .beforeMetricsJson("{}")
                .executedAt(LocalDateTime.now())
                .verificationDueAt(LocalDateTime.now().plusDays(14))
                .build();
        when(executionRepository.findByRecommendationRun_ThreadIdAndUser_Id(threadId, USER_ID))
                .thenReturn(Optional.of(execution));

        VerificationExecutionResponse response = lifecycleService.linkCampaignDecision(
                USER_ID,
                threadId,
                decisionId
        );

        assertThat(response.getDecisionId()).isEqualTo(decisionId);
        verify(executionRepository, never()).save(execution);
    }

    @Test
    void completeVerificationRejectsCollectionBeforeDueDate() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 20, 10, 0);
        EffectVerificationExecution execution = savedExecution(executedAt);
        when(executionRepository.findByAiRecommendationIdAndUser_Id("100", USER_ID))
                .thenReturn(Optional.of(execution));
        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(salesPeriod(1_300_000.0));
        request.setCollectedAt(executedAt.plusDays(13));

        assertThatThrownBy(() ->
                lifecycleService.completeVerification(USER_ID, "100", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("collection period has not ended");
    }

    @Test
    void completeVerificationBuildsAiRequestFromStoredBaseline() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        EffectVerificationExecution execution = savedExecution(executedAt);
        when(executionRepository.findByAiRecommendationIdAndUser_Id("100", USER_ID))
                .thenReturn(Optional.of(execution));
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime verifiedAt = LocalDateTime.of(2026, 7, 20, 11, 0);
        EffectVerificationResponse aiResponse = new EffectVerificationResponse();
        aiResponse.setVerifiedDate(verifiedAt);
        when(verificationService.verifyEffect(
                any(Long.class),
                any(EffectVerificationRequest.class)
        ))
                .thenReturn(aiResponse);

        VerificationCompletionRequest request = new VerificationCompletionRequest();
        request.setAfter(salesPeriod(1_300_000.0));
        request.setCollectedAt(executedAt.plusDays(14));

        lifecycleService.completeVerification(USER_ID, "100", request);

        ArgumentCaptor<EffectVerificationRequest> captor =
                ArgumentCaptor.forClass(EffectVerificationRequest.class);
        verify(verificationService).verifyEffect(
                any(Long.class),
                captor.capture()
        );
        EffectVerificationRequest sent = captor.getValue();
        assertThat(sent.getRecommendationId()).isEqualTo("100");
        assertThat(sent.getBefore().getSales().getTargetSales()).isEqualTo(1_000_000.0);
        assertThat(sent.getAfter().getSales().getTargetSales()).isEqualTo(1_300_000.0);
        assertThat(execution.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(execution.getVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    void getDueExecutionsReturnsCollectingExecutionsForStore() {
        LocalDateTime executedAt = LocalDateTime.now().minusDays(15);
        EffectVerificationExecution execution = savedExecution(executedAt);
        when(executionRepository
                .findByUser_IdAndStore_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(Long.class),
                        any(Long.class),
                        any(VerificationStatus.class),
                        any(LocalDateTime.class)
                ))
                .thenReturn(List.of(execution));

        List<VerificationExecutionResponse> responses =
                lifecycleService.getDueExecutions(USER_ID, 1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getRecommendationId()).isEqualTo("100");
        assertThat(responses.getFirst().getStatus()).isEqualTo(VerificationStatus.COLLECTING);
        verify(executionRepository)
                .findByUser_IdAndStore_IdAndStatusAndVerificationDueAtLessThanEqualOrderByVerificationDueAtAsc(
                        any(Long.class),
                        any(Long.class),
                        any(VerificationStatus.class),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void getExecutionHistoryFiltersByStoreAndStatus() {
        EffectVerificationExecution execution = savedExecution(
                LocalDateTime.of(2026, 7, 1, 10, 0)
        );
        when(executionRepository.findByUser_IdAndStore_IdAndStatusOrderByExecutedAtDesc(
                USER_ID,
                1L,
                VerificationStatus.COLLECTING
        )).thenReturn(List.of(execution));

        List<VerificationExecutionResponse> responses =
                lifecycleService.getExecutionHistory(
                        USER_ID,
                        1L,
                        VerificationStatus.COLLECTING
                );

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getStoreId()).isEqualTo(1L);
        assertThat(responses.getFirst().getStatus()).isEqualTo(VerificationStatus.COLLECTING);
        verify(executionRepository).findByUser_IdAndStore_IdAndStatusOrderByExecutedAtDesc(
                USER_ID,
                1L,
                VerificationStatus.COLLECTING
        );
        verify(executionRepository, never())
                .findByUser_IdAndStore_IdOrderByExecutedAtDesc(USER_ID, 1L);
    }

    @Test
    void reviewExecutionCompletesWithStoredReviewBaseline() {
        LocalDateTime executedAt = LocalDateTime.of(2026, 7, 1, 10, 0);
        ExecutionRegistrationRequest registration = new ExecutionRegistrationRequest();
        registration.setStoreId(1L);
        registration.setRecommendationId("200");
        registration.setRecommendationType(RecommendationType.REVIEW);
        registration.setCondition(new VerificationCondition(
                14,
                null,
                null,
                true,
                "waiting_time"
        ));
        registration.setBefore(reviewPeriod(3.8, 40.0));
        registration.setExecutedAt(executedAt);

        when(executionRepository.existsByAiRecommendationId("200")).thenReturn(false);
        when(executionRepository.save(any(EffectVerificationExecution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        lifecycleService.registerExecution(USER_ID, registration);

        ArgumentCaptor<EffectVerificationExecution> executionCaptor =
                ArgumentCaptor.forClass(EffectVerificationExecution.class);
        verify(executionRepository).save(executionCaptor.capture());
        EffectVerificationExecution saved = executionCaptor.getValue();
        when(executionRepository.findByAiRecommendationIdAndUser_Id("200", USER_ID))
                .thenReturn(Optional.of(saved));

        EffectVerificationResponse aiResponse = new EffectVerificationResponse();
        aiResponse.setVerifiedDate(executedAt.plusDays(15));
        when(verificationService.verifyEffect(
                any(Long.class),
                any(EffectVerificationRequest.class)
        ))
                .thenReturn(aiResponse);

        VerificationCompletionRequest completion = new VerificationCompletionRequest();
        completion.setAfter(reviewPeriod(4.4, 18.0));
        completion.setCollectedAt(executedAt.plusDays(15));

        lifecycleService.completeVerification(USER_ID, "200", completion);

        ArgumentCaptor<EffectVerificationRequest> requestCaptor =
                ArgumentCaptor.forClass(EffectVerificationRequest.class);
        verify(verificationService).verifyEffect(
                any(Long.class),
                requestCaptor.capture()
        );
        EffectVerificationRequest sent = requestCaptor.getValue();
        assertThat(sent.getRecommendationType()).isEqualTo(RecommendationType.REVIEW);
        assertThat(sent.getCondition().getTargetAspect()).isEqualTo("waiting_time");
        assertThat(sent.getBefore().getReview().getAverageRating()).isEqualTo(3.8);
        assertThat(sent.getAfter().getReview().getAverageRating()).isEqualTo(4.4);
        assertThat(saved.getStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void reviewRegistrationRejectsMissingTargetAspect() {
        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setStoreId(1L);
        request.setRecommendationId("201");
        request.setRecommendationType(RecommendationType.REVIEW);
        request.setCondition(new VerificationCondition(14, null, null, true, null));
        request.setBefore(reviewPeriod(3.8, 40.0));

        assertThatThrownBy(() -> lifecycleService.registerExecution(USER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("requires target_aspect");
    }

    private ExecutionRegistrationRequest registrationRequest(LocalDateTime executedAt) {
        ExecutionRegistrationRequest request = new ExecutionRegistrationRequest();
        request.setStoreId(1L);
        request.setRecommendationId("100");
        request.setRecommendationType(RecommendationType.SALES);
        request.setCondition(new VerificationCondition(14, 14, 17, true, null));
        request.setBefore(salesPeriod(1_000_000.0));
        request.setExecutedAt(executedAt);
        return request;
    }

    private EffectVerificationExecution savedExecution(LocalDateTime executedAt) {
        return EffectVerificationExecution.builder()
                .aiRecommendationId("100")
                .user(user)
                .store(store)
                .recommendationType(RecommendationType.SALES)
                .status(VerificationStatus.COLLECTING)
                .conditionJson("{\"period_days\":14,\"start_hour\":14,\"end_hour\":17,\"compare_same_weekday\":true}")
                .beforeMetricsJson("{\"sales\":{\"target_sales\":1000000.0,\"visit_count\":100,\"average_order_value\":10000.0,\"revisit_rate\":20.0,\"coupon_usage_rate\":10.0,\"new_customer_count\":20,\"dormant_customer_return_count\":5,\"total_sales\":5000000.0},\"review\":null}")
                .executedAt(executedAt)
                .verificationDueAt(executedAt.plusDays(14))
                .build();
    }

    private PeriodMetrics salesPeriod(double targetSales) {
        SalesMetrics sales = new SalesMetrics(
                targetSales,
                100,
                10_000.0,
                20.0,
                10.0,
                20,
                5,
                5_000_000.0
        );
        return new PeriodMetrics(sales, null);
    }

    private PeriodMetrics reviewPeriod(double averageRating, double negativeRate) {
        ReviewMetrics review = new ReviewMetrics(
                averageRating,
                negativeRate,
                20,
                negativeRate,
                0.9,
                50,
                25.0,
                5_000_000.0
        );
        return new PeriodMetrics(null, review);
    }
}
