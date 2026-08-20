package com.bp20.backend.global.security.captcha;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class CaptchaVerificationService {

    private final CaptchaProperties properties;
    private final RestClient restClient;

    public CaptchaVerificationService(
            CaptchaProperties properties,
            @Qualifier("restClientBuilder") RestClient.Builder builder
    ) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    public void verify(String responseToken, String remoteIp) {
        verify(responseToken, remoteIp, properties.expectedAction());
    }

    public void verify(String responseToken, String remoteIp, String expectedAction) {
        if (!properties.enabled()) {
            return;
        }
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            throw new IllegalStateException("CAPTCHA_SECRET_KEY must be configured when CAPTCHA is enabled.");
        }
        if (responseToken == null || responseToken.isBlank()) {
            throw new ApiException(ErrorCode.BAD_REQUEST_CAPTCHA_REQUIRED);
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.secretKey());
        form.add("response", responseToken);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form.add("remoteip", remoteIp);
        }

        CaptchaVerificationResponse response;
        try {
            response = restClient.post()
                    .uri(properties.verifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(CaptchaVerificationResponse.class);
        } catch (RuntimeException exception) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE_CAPTCHA);
        }
        if (response == null) {
            log.warn("reCAPTCHA verification returned an empty response. expectedAction={}", expectedAction);
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_CAPTCHA);
        }

        boolean allowedHostname = isAllowedHostname(response.hostname());
        if (!response.success()
                || response.score() == null
                || response.score() < properties.minimumScore()
                || !expectedAction.equals(response.action())
                || !allowedHostname) {
            // CAPTCHA 응답 토큰과 Secret은 민감 정보이므로 로그에 남기지 않습니다.
            log.warn(
                    "reCAPTCHA verification rejected. success={}, score={}, action={}, expectedAction={}, hostname={}, allowedHostname={}, errorCodes={}",
                    response.success(),
                    response.score(),
                    response.action(),
                    expectedAction,
                    response.hostname(),
                    allowedHostname,
                    response.errorCodes()
            );
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_CAPTCHA);
        }
    }

    private boolean isAllowedHostname(String hostname) {
        if (properties.allowedHostnames().isEmpty()) {
            return true;
        }
        return hostname != null && properties.allowedHostnames().stream()
                .anyMatch(allowedHostname -> allowedHostname.equalsIgnoreCase(hostname));
    }

    private record CaptchaVerificationResponse(
            boolean success,
            Double score,
            String action,
            @JsonProperty("challenge_ts") String challengeTimestamp,
            String hostname,
            @JsonProperty("error-codes") List<String> errorCodes
    ) {
    }
}
