package com.bp20.backend.api.notice.dto.request;

import com.bp20.backend.api.notice.domain.NoticeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        @NotBlank @Size(max = 30) String category,
        @NotBlank @Size(max = 30) String audience,
        @NotNull NoticeStatus status,
        boolean urgent
) {
}
