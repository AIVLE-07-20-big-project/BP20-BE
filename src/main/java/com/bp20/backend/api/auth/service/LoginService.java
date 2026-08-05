package com.bp20.backend.api.auth.service;

import com.bp20.backend.api.auth.dto.request.LoginRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.dto.response.MeResponse;
import com.bp20.backend.api.auth.session.AuthenticatedSession;
import com.bp20.backend.api.auth.session.RefreshTokenService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.security.account.AccountSecurityProperties;
import com.bp20.backend.global.security.captcha.CaptchaVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final AccountSecurityProperties accountSecurityProperties;
    private final CaptchaVerificationService captchaVerificationService;

    @Transactional(noRollbackFor = ApiException.class)
    public AuthenticatedSession<LoginResponse> login(LoginRequest request, String sourceIp) {
        captchaVerificationService.verify(request.captchaToken(), sourceIp);
        String email = normalizeEmail(request.email());
        LocalDateTime now = LocalDateTime.now();
        User user = userRepository.findByEmailForAuthentication(email)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED_INVALID_CREDENTIALS));

        if (user.isTemporarilyLocked(now)) {
            throw new ApiException(ErrorCode.LOCKED_LOGIN_ACCOUNT);
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            user.registerFailedLogin(
                    accountSecurityProperties.maxFailedLoginAttempts(),
                    accountSecurityProperties.accountLockDuration(),
                    now
            );
            if (user.isTemporarilyLocked(now)) {
                throw new ApiException(ErrorCode.LOCKED_LOGIN_ACCOUNT);
            }
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_CREDENTIALS);
        }

        user.loginSucceeded();
        if (user.isPasswordExpired(now, accountSecurityProperties.passwordMaxAge())) {
            throw new ApiException(ErrorCode.FORBIDDEN_PASSWORD_EXPIRED);
        }

        RefreshTokenService.TokenPair tokenPair =
                refreshTokenService.issue(user, request.rememberMe());
        return AuthenticatedSession.of(
                LoginResponse.of(tokenPair.accessToken(), user),
                tokenPair
        );
    }

    public AuthenticatedSession<LoginResponse> login(LoginRequest request) {
        return login(request, null);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));

        return MeResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
