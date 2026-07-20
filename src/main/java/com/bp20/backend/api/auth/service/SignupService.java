package com.bp20.backend.api.auth.service;

import com.bp20.backend.api.auth.dto.request.SignupRequest;
import com.bp20.backend.api.auth.dto.response.SignupResponse;
import com.bp20.backend.api.iam.invitation.domain.Invitation;
import com.bp20.backend.api.iam.invitation.repository.InvitationRepository;
import com.bp20.backend.api.iam.invitation.service.TemporaryPasswordService;
import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.iam.log.service.IamLogService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;
    private final JwtTokenProvider jwtTokenProvider;
    private final IamLogService iamLogService;

    @Transactional
    public SignupResponse signup(SignupRequest request, String sourceIp) {
        String email = normalizeEmail(request.email());
        Invitation invitation = invitationRepository
                .findByEmailAndTemporaryPasswordHashAndAcceptedAtIsNullAndRevokedAtIsNull(
                        email,
                        temporaryPasswordService.hash(request.temporaryPassword())
                )
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_SIGNUP_INVITATION));

        LocalDateTime now = LocalDateTime.now();
        if (!invitation.isUsable(now)) {
            throw new ApiException(ErrorCode.CONFLICT_EXPIRED_SIGNUP_INVITATION);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }

        User user = userRepository.save(createInvitedUser(invitation.getTargetRole(), request, email));
        invitation.accept(now);
        iamLogService.record(
                invitation.getInvitedBy().getId(),
                invitationAcceptedAction(invitation.getTargetRole()),
                user.getId(),
                user.getEmail(),
                sourceIp
        );

        return SignupResponse.of(jwtTokenProvider.createAccessToken(user), user);
    }

    private User createInvitedUser(UserRole targetRole, SignupRequest request, String email) {
        String name = request.name().trim();
        String phoneNumber = trimToNull(request.phoneNumber());
        String passwordHash = passwordEncoder.encode(request.password());

        return switch (targetRole) {
            case ADMIN -> User.createAdmin(email, name, phoneNumber, passwordHash);
            case STORE_OWNER -> User.createStoreOwner(email, name, phoneNumber, passwordHash);
            default -> throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ROLE);
        };
    }

    private IamLogAction invitationAcceptedAction(UserRole targetRole) {
        return targetRole == UserRole.ADMIN
                ? IamLogAction.ADMIN_INVITATION_ACCEPTED
                : IamLogAction.STORE_OWNER_INVITATION_ACCEPTED;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
