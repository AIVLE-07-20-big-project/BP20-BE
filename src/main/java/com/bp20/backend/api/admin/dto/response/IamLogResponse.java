package com.bp20.backend.api.admin.dto.response;

import com.bp20.backend.api.iam.domain.IamLog;
import com.bp20.backend.api.iam.domain.IamLogAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "IAM 로그 응답")
public record IamLogResponse(
        @Schema(description = "IAM 로그 ID", example = "1")
        Long id,
        @Schema(description = "작업 수행 관리자 ID. 최초 관리자 CLI 생성 작업이면 null일 수 있습니다.", example = "1")
        Long actorUserId,
        @Schema(description = "IAM 작업 유형", example = "ADMIN_INVITATION_CREATED")
        IamLogAction action,
        @Schema(description = "대상 사용자 ID", example = "2")
        Long targetUserId,
        @Schema(description = "대상 이메일", example = "admin@bp20.com")
        String targetEmail,
        @Schema(description = "요청 출발지 IP", example = "127.0.0.1")
        String sourceIp,
        @Schema(description = "작업 일시")
        LocalDateTime createdAt
) {
    public static IamLogResponse from(IamLog log) {
        return new IamLogResponse(
                log.getId(), log.getActorUserId(), log.getAction(), log.getTargetUserId(),
                log.getTargetEmail(), log.getSourceIp(), log.getCreatedAt()
        );
    }
}
