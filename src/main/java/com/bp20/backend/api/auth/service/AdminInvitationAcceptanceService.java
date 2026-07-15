package com.bp20.backend.api.auth.service;

import com.bp20.backend.api.auth.dto.request.AcceptAdminInvitationRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.invitation.domain.AdminInvitation;
import com.bp20.backend.api.iam.domain.IamLogAction;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import com.bp20.backend.api.invitation.repository.AdminInvitationRepository;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.api.iam.service.IamLogService;
import com.bp20.backend.api.invitation.service.TemporaryPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminInvitationAcceptanceService {

    private final UserRepository userRepository;
    private final AdminInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final IamLogService iamLogService;
    private final TemporaryPasswordService temporaryPasswordService;

    @Transactional
    public LoginResponse acceptInvitation(AcceptAdminInvitationRequest request, String sourceIp) {
        String email = normalizeEmail(request.email());
        AdminInvitation invitation = invitationRepository
                .findByEmailAndTemporaryPasswordHashAndAcceptedAtIsNullAndRevokedAtIsNull(
                        email,
                        temporaryPasswordService.hash(request.temporaryPassword())
                )
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_ADMIN_INVITATION));

        LocalDateTime now = LocalDateTime.now();
        if (!invitation.isUsable(now)) {
            throw new ApiException(ErrorCode.CONFLICT_EXPIRED_ADMIN_INVITATION);
        }
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }

        User admin = userRepository.save(User.createAdmin(
                invitation.getEmail(),
                request.name().trim(),
                trimToNull(request.phoneNumber()),
                passwordEncoder.encode(request.newPassword())
        ));
        invitation.accept(now);
        iamLogService.record(invitation.getInvitedBy().getId(), IamLogAction.ADMIN_INVITATION_ACCEPTED,
                admin.getId(), admin.getEmail(), sourceIp);

        return LoginResponse.of(jwtTokenProvider.createAccessToken(admin), admin);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
