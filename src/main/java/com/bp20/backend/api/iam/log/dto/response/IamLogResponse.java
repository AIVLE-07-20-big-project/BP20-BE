package com.bp20.backend.api.iam.log.dto.response;

import com.bp20.backend.api.iam.log.domain.IamLog;
import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.global.util.PersonalDataMasker;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "IAM 로그 응답")
public record IamLogResponse(
        Long id, Long actorUserId, IamLogAction action, Long targetUserId,
        String targetEmail, String sourceIp, LocalDateTime createdAt
) {
    public static IamLogResponse from(IamLog log) {
        return new IamLogResponse(
                log.getId(),
                log.getActorUser() == null ? null : log.getActorUser().getId(),
                log.getAction(),
                log.getTargetUser() == null ? null : log.getTargetUser().getId(),
                PersonalDataMasker.email(log.getTargetEmail()),
                PersonalDataMasker.ipAddress(log.getSourceIp()),
                log.getCreatedAt()
        );
    }
}
