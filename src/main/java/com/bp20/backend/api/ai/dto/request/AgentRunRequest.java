package com.bp20.backend.api.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AgentRunRequest(
        @NotBlank @JsonProperty("trdar_cd") String trdarCd,
        @NotBlank @JsonProperty("svc_induty_cd") String svcIndutyCd,
        @JsonProperty("yyqu_cd") Integer yyquCd
) {
}
