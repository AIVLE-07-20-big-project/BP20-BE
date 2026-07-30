package com.bp20.backend.api.customer.controller;

import com.bp20.backend.api.customer.dto.request.CreateCustomerRequest;
import com.bp20.backend.api.customer.dto.response.CustomerResponse;
import com.bp20.backend.api.customer.service.CustomerService;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store-owner/stores/me/customers")
@Tag(name = "점주 - 고객 관리", description = "매장 고객을 등록하고 조회하는 API")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(
            summary = "고객 등록",
            description = "현재 점주의 매장에 고객을 등록합니다. 같은 매장에서는 동일한 이메일을 중복 등록할 수 없습니다."
    )
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_CREATE,
                customerService.create(currentUser.id(), request)
        );
    }

    @GetMapping
    @Operation(summary = "고객 목록 조회", description = "현재 점주의 매장 고객을 최신 등록순으로 조회합니다.")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getMine(
            @AuthenticationPrincipal SecurityPrincipal currentUser
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_GET,
                customerService.getMine(currentUser.id())
        );
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "고객 상세 조회", description = "현재 점주의 매장에 등록된 고객 한 명을 조회합니다.")
    public ResponseEntity<ApiResponse<CustomerResponse>> getOne(
            @AuthenticationPrincipal SecurityPrincipal currentUser,
            @PathVariable Long customerId
    ) {
        return ApiResponse.success(
                SuccessCode.SUCCESS_CUSTOMER_GET,
                customerService.getOne(currentUser.id(), customerId)
        );
    }
}
