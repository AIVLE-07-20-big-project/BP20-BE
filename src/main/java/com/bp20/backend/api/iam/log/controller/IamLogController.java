package com.bp20.backend.api.iam.log.controller;

import com.bp20.backend.api.iam.log.dto.response.IamLogResponse;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.response.SuccessCode;
import com.bp20.backend.api.iam.log.service.IamLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/iam/logs")
@Tag(name = "IAM - 로그", description = "최상위 관리자가 IAM 관리 작업 기록을 조회하는 API")
@SecurityRequirement(name = "bearerAuth")
public class IamLogController {

    private final IamLogService iamLogService;

    @GetMapping
    @Operation(summary = "IAM 로그 조회", description = "최근 IAM 관리 작업 로그 100건을 최신순으로 조회합니다.")
    public ResponseEntity<ApiResponse<List<IamLogResponse>>> getLogs() {
        List<IamLogResponse> logs = iamLogService.getRecentLogs().stream()
                .map(IamLogResponse::from)
                .toList();
        return ApiResponse.success(SuccessCode.SUCCESS_IAM_LOG_GET, logs);
    }
}
