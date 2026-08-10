package com.bp20.backend.api.store.controller;

import com.bp20.backend.api.store.dto.response.StoreRegistryEntryResponse;
import com.bp20.backend.api.store.service.InternalStoreRegistryService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 서버(FastAPI) 전용 내부 API. 사용자 JWT가 아니라 InternalApiKeyFilter가 검증하는
 * X-Internal-Api-Key 헤더로 인증한다(SecurityConfig의 "/api/internal/**" permitAll 설정과 짝을 이룬다).
 *
 * 신규 가맹점 영업 타겟 추천 배치가 공공데이터 상가업소 목록에서 "이미 자사 가맹점인 업장"을
 * 걸러낼 때 호출한다. 사업자등록번호가 없는 데이터셋이라 실제 매칭은 address(주소) 정규화가 주력이 될
 * 가능성이 높다 — 이 API는 매칭에 필요한 최소 필드(사업자번호/상호명/업종/주소)만 내려준다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/stores")
@Tag(name = "내부 - 가맹점 레지스트리", description = "AI 서버가 신규 영업 타겟 매칭에 쓰는 자사 가맹점 목록 조회 API")
public class InternalStoreRegistryController {

    private final InternalStoreRegistryService internalStoreRegistryService;

    @GetMapping("/registry")
    @Operation(
            summary = "자사 가맹점 목록 조회(내부용)",
            description = "신규 가맹점 영업 타겟 추천 배치가 이미 가입한 업장을 걸러낼 때 사용한다. X-Internal-Api-Key 헤더 필요."
    )
    public ResponseEntity<ApiResponse<List<StoreRegistryEntryResponse>>> getRegistry() {
        return ApiResponse.success(
                SuccessCode.SUCCESS_STORE_REGISTRY_GET,
                internalStoreRegistryService.getAllForRegistry()
        );
    }
}
