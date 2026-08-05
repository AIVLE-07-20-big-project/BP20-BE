package com.bp20.backend.api.salestarget.service;

import com.bp20.backend.api.salestarget.client.SalesTargetAiClient;
import com.bp20.backend.api.salestarget.domain.SalesTargetBatchRun;
import com.bp20.backend.api.salestarget.repository.SalesTargetBatchRunRepository;
import com.bp20.backend.global.exception.ApiException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ObjectMapper는 실제 JSON 직렬화/역직렬화가 필요해서(Map<String,Object> <-> LONGTEXT 왕복)
// 목이 아니라 진짜 인스턴스를 쓴다 — SalesTargetAiClient/Repository만 목으로 대체한다.
@ExtendWith(MockitoExtension.class)
class SalesTargetBatchServiceTest {

    @Mock
    private SalesTargetAiClient salesTargetAiClient;

    @Mock
    private SalesTargetBatchRunRepository batchRunRepository;

    private SalesTargetBatchService service;

    @BeforeEach
    void setUp() {
        service = new SalesTargetBatchService(salesTargetAiClient, batchRunRepository, new ObjectMapper());
    }

    @Test
    void 배치를_시작하면_thread_id로_실행기록을_저장한다() {
        Map<String, Object> aiResponse = Map.of(
                "thread_id", "thread-1",
                "상태", "스코어링 완료 — 관리자 승인 대기",
                "대기중_승인", Map.of("후보_수", 3)
        );
        when(salesTargetAiClient.startBatch(20)).thenReturn(aiResponse);

        Map<String, Object> result = service.startBatch(1L, 20);

        assertThat(result.get("thread_id")).isEqualTo("thread-1");
        ArgumentCaptor<SalesTargetBatchRun> captor = ArgumentCaptor.forClass(SalesTargetBatchRun.class);
        verify(batchRunRepository).save(captor.capture());
        assertThat(captor.getValue().getThreadId()).isEqualTo("thread-1");
        assertThat(captor.getValue().getTriggeredByAdminId()).isEqualTo(1L);
    }

    @Test
    void thread_id가_없는_AI_응답이면_예외를_던진다() {
        when(salesTargetAiClient.startBatch(any())).thenReturn(Map.of("상태", "오류"));

        assertThatThrownBy(() -> service.startBatch(1L, null))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 존재하지_않는_thread_id를_조회하면_예외가_발생한다() {
        when(batchRunRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBatch("missing"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void 승인하면_AI에_approve를_호출하고_실행기록을_갱신한다() {
        SalesTargetBatchRun run = SalesTargetBatchRun.create("thread-2", 1L, "{}");
        when(batchRunRepository.findById("thread-2")).thenReturn(Optional.of(run));
        Map<String, Object> aiResponse = Map.of("상태", "완료 — BE 반영됨", "thread_id", "thread-2");
        when(salesTargetAiClient.approveBatch("thread-2")).thenReturn(aiResponse);

        Map<String, Object> result = service.approveBatch("thread-2");

        assertThat(result.get("상태")).isEqualTo("완료 — BE 반영됨");
        assertThat(run.getResultJson()).contains("완료 — BE 반영됨");
    }

    @Test
    void 반려하면_AI에_reject를_호출하고_실행기록을_갱신한다() {
        SalesTargetBatchRun run = SalesTargetBatchRun.create("thread-3", 1L, "{}");
        when(batchRunRepository.findById("thread-3")).thenReturn(Optional.of(run));
        Map<String, Object> aiResponse = Map.of("상태", "종료: 반려되어 BE에 반영하지 않음");
        when(salesTargetAiClient.rejectBatch("thread-3")).thenReturn(aiResponse);

        service.rejectBatch("thread-3");

        verify(salesTargetAiClient).rejectBatch("thread-3");
        assertThat(run.getResultJson()).contains("반려");
    }

    @Test
    void 목록조회는_최근순으로_받은_실행기록을_역직렬화해서_반환한다() {
        SalesTargetBatchRun run = SalesTargetBatchRun.create(
                "thread-4", 1L, "{\"상태\":\"스코어링 완료 — 관리자 승인 대기\"}"
        );
        when(batchRunRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(run));

        List<Map<String, Object>> result = service.listBatches();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("thread_id")).isEqualTo("thread-4");
        assertThat(result.get(0).get("상태")).isEqualTo("스코어링 완료 — 관리자 승인 대기");
    }

    @Test
    void 목록조회_응답에_자동반려_여부가_포함된다() {
        SalesTargetBatchRun run = SalesTargetBatchRun.create(
                "thread-flag", 1L, "{\"상태\":\"종료: 반려되어 BE에 반영하지 않음\"}"
        );
        run.markAutoRejected();
        when(batchRunRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(run));

        List<Map<String, Object>> result = service.listBatches();

        assertThat(result.get(0).get("auto_rejected")).isEqualTo(true);
    }

    // 4단계(운영 정리 정책) — autoRejectStaleBatches

    @Test
    void 오래_방치된_대기중_배치는_재조회후에도_대기중이면_자동반려한다() {
        SalesTargetBatchRun run = SalesTargetBatchRun.create(
                "thread-stale", 1L, "{\"대기중_승인\":{\"후보_수\":5}}"
        );
        when(batchRunRepository.findAllByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(run));
        Map<String, Object> stillPending = Map.of(
                "thread_id", "thread-stale", "대기중_승인", Map.of("후보_수", 5)
        );
        when(salesTargetAiClient.getBatch("thread-stale")).thenReturn(stillPending);
        when(salesTargetAiClient.rejectBatch("thread-stale"))
                .thenReturn(Map.of("상태", "종료: 반려되어 BE에 반영하지 않음"));

        List<String> rejected = service.autoRejectStaleBatches(3);

        assertThat(rejected).containsExactly("thread-stale");
        assertThat(run.isAutoRejected()).isTrue();
        verify(salesTargetAiClient).rejectBatch("thread-stale");
    }

    @Test
    void 오래됐지만_캐시상_이미_끝난_배치는_AI를_다시_조회하지_않는다() {
        SalesTargetBatchRun run = SalesTargetBatchRun.create(
                "thread-done", 1L, "{\"상태\":\"완료 — BE 반영됨\"}"
        );
        when(batchRunRepository.findAllByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(run));

        List<String> rejected = service.autoRejectStaleBatches(3);

        assertThat(rejected).isEmpty();
        assertThat(run.isAutoRejected()).isFalse();
        verify(salesTargetAiClient, never()).getBatch(any());
        verify(salesTargetAiClient, never()).rejectBatch(any());
    }

    @Test
    void 재조회했더니_그사이_이미_처리된_배치는_반려호출하지_않는다() {
        // 캐시(resultJson)엔 대기 중으로 남아있지만, 그 사이 관리자가 직접 승인/반려했거나 다른
        // 경로로 상태가 바뀐 상황을 흉내낸다 — autoRejectStaleBatches는 재조회 결과를 우선해야 한다.
        SalesTargetBatchRun run = SalesTargetBatchRun.create(
                "thread-race", 1L, "{\"대기중_승인\":{\"후보_수\":2}}"
        );
        when(batchRunRepository.findAllByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(List.of(run));
        Map<String, Object> alreadyResolved = Map.of("상태", "완료 — BE 반영됨");
        when(salesTargetAiClient.getBatch("thread-race")).thenReturn(alreadyResolved);

        List<String> rejected = service.autoRejectStaleBatches(3);

        assertThat(rejected).isEmpty();
        assertThat(run.isAutoRejected()).isFalse();
        assertThat(run.getResultJson()).contains("완료");
        verify(salesTargetAiClient, never()).rejectBatch(any());
    }

    @Test
    void 자동반려_대상이_여러건이면_전부_반려하고_목록으로_반환한다() {
        SalesTargetBatchRun run1 = SalesTargetBatchRun.create("thread-a", 1L, "{\"대기중_승인\":{}}");
        SalesTargetBatchRun run2 = SalesTargetBatchRun.create("thread-b", 1L, "{\"대기중_승인\":{}}");
        when(batchRunRepository.findAllByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(run1, run2));
        when(salesTargetAiClient.getBatch(any())).thenReturn(Map.of("대기중_승인", Map.of()));
        when(salesTargetAiClient.rejectBatch(any())).thenReturn(Map.of("상태", "종료: 반려되어 BE에 반영하지 않음"));

        List<String> rejected = service.autoRejectStaleBatches(3);

        assertThat(rejected).containsExactlyInAnyOrder("thread-a", "thread-b");
        assertThat(run1.isAutoRejected()).isTrue();
        assertThat(run2.isAutoRejected()).isTrue();
    }
}
