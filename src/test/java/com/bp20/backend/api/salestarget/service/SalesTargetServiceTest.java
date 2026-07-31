package com.bp20.backend.api.salestarget.service;

import com.bp20.backend.api.salestarget.domain.PipelineStatus;
import com.bp20.backend.api.salestarget.domain.SalesTargetCandidate;
import com.bp20.backend.api.salestarget.dto.request.BulkUpsertSalesTargetsRequest;
import com.bp20.backend.api.salestarget.dto.request.SalesTargetItemRequest;
import com.bp20.backend.api.salestarget.dto.response.SalesTargetCandidateResponse;
import com.bp20.backend.api.salestarget.repository.SalesTargetCandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesTargetServiceTest {

    @Mock
    private SalesTargetCandidateRepository salesTargetCandidateRepository;

    @InjectMocks
    private SalesTargetService service;

    private SalesTargetCandidate candidate(Long id, String name, String address, PipelineStatus status) {
        SalesTargetCandidate candidate = mock(SalesTargetCandidate.class);
        when(candidate.getId()).thenReturn(id);
        when(candidate.getBusinessName()).thenReturn(name);
        when(candidate.getAddress()).thenReturn(address);
        when(candidate.getPipelineStatus()).thenReturn(status);
        return candidate;
    }

    // 벌크 업서트 테스트 전용 — refreshScores()/updatePipelineStatus() 호출 여부만 검증하면 되고
    // 어떤 게터도 실제로 호출되지 않으므로 아무것도 스텁하지 않는다.
    private SalesTargetCandidate bareMock() {
        return mock(SalesTargetCandidate.class);
    }

    @Test
    void 상태_필터_없이_조회하면_전체를_점수순으로_받는다() {
        SalesTargetCandidate a = candidate(1L, "A", "주소A", PipelineStatus.CANDIDATE);
        List<SalesTargetCandidate> all = List.of(a);
        when(salesTargetCandidateRepository.findAllByOrderByTotalScoreDesc()).thenReturn(all);

        List<SalesTargetCandidateResponse> result = service.getAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("A");
    }

    @Test
    void 상태_필터로_조회하면_해당_상태만_받는다() {
        SalesTargetCandidate contacted = candidate(2L, "B", "주소B", PipelineStatus.CONTACTED);
        List<SalesTargetCandidate> filtered = List.of(contacted);
        when(salesTargetCandidateRepository.findAllByPipelineStatusOrderByTotalScoreDesc(PipelineStatus.CONTACTED))
                .thenReturn(filtered);

        List<SalesTargetCandidateResponse> result = service.getAll(PipelineStatus.CONTACTED);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).pipelineStatus()).isEqualTo(PipelineStatus.CONTACTED);
    }

    @Test
    void 존재하지_않는_id로_조회하면_예외가_발생한다() {
        when(salesTargetCandidateRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 파이프라인_상태를_변경한다() {
        SalesTargetCandidate stored = candidate(1L, "A", "주소A", PipelineStatus.CANDIDATE);
        when(salesTargetCandidateRepository.findById(1L)).thenReturn(Optional.of(stored));

        service.updatePipelineStatus(1L, PipelineStatus.CONTACTED);

        verify(stored).updatePipelineStatus(PipelineStatus.CONTACTED);
    }

    @Test
    void 새_후보는_CANDIDATE_상태로_생성된다() {
        when(salesTargetCandidateRepository.findByBusinessNameAndAddress("신규카페", "서울 강남구 1"))
                .thenReturn(Optional.empty());

        SalesTargetItemRequest item = new SalesTargetItemRequest(
                "신규카페", "카페", "서울 강남구 1", 76.1, 100.0, 94.4, 50.0, 50.0
        );
        BulkUpsertSalesTargetsRequest request = new BulkUpsertSalesTargetsRequest("batch-1", List.of(item));

        SalesTargetService.BulkUpsertResult result = service.bulkUpsertCandidates(request);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(0);

        ArgumentCaptor<SalesTargetCandidate> captor = ArgumentCaptor.forClass(SalesTargetCandidate.class);
        verify(salesTargetCandidateRepository).save(captor.capture());
        assertThat(captor.getValue().getPipelineStatus()).isEqualTo(PipelineStatus.CANDIDATE);
    }

    @Test
    void 기존_후보는_점수만_갱신되고_이미_진행중인_상태는_유지된다() {
        SalesTargetCandidate existing = bareMock();
        when(salesTargetCandidateRepository.findByBusinessNameAndAddress("기존카페", "서울 강남구 2"))
                .thenReturn(Optional.of(existing));

        SalesTargetItemRequest item = new SalesTargetItemRequest(
                "기존카페", "카페", "서울 강남구 2", 80.0, 100.0, 90.0, 60.0, 70.0
        );
        BulkUpsertSalesTargetsRequest request = new BulkUpsertSalesTargetsRequest("batch-2", List.of(item));

        SalesTargetService.BulkUpsertResult result = service.bulkUpsertCandidates(request);

        assertThat(result.created()).isEqualTo(0);
        assertThat(result.updated()).isEqualTo(1);

        verify(existing).refreshScores("카페", 80.0, 100.0, 90.0, 60.0, 70.0, "batch-2");
        // pipelineStatus 변경 메서드는 절대 호출되면 안 된다 — 벌크 업서트가 상태를 건드리지 않는다는 핵심 보장.
        verify(existing, never()).updatePipelineStatus(any());
        verify(salesTargetCandidateRepository, never()).save(any());
    }

    @Test
    void 여러_건_중_일부는_생성되고_일부는_갱신된다() {
        SalesTargetCandidate existing = bareMock();
        when(salesTargetCandidateRepository.findByBusinessNameAndAddress("기존", "주소1"))
                .thenReturn(Optional.of(existing));
        when(salesTargetCandidateRepository.findByBusinessNameAndAddress("신규", "주소2"))
                .thenReturn(Optional.empty());

        List<SalesTargetItemRequest> items = List.of(
                new SalesTargetItemRequest("기존", "카페", "주소1", 70.0, 80.0, 70.0, 50.0, 50.0),
                new SalesTargetItemRequest("신규", "정육점", "주소2", 60.0, 60.0, 60.0, 50.0, 50.0)
        );
        BulkUpsertSalesTargetsRequest request = new BulkUpsertSalesTargetsRequest("batch-3", items);

        SalesTargetService.BulkUpsertResult result = service.bulkUpsertCandidates(request);

        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        verify(salesTargetCandidateRepository, times(1)).save(any());
    }
}