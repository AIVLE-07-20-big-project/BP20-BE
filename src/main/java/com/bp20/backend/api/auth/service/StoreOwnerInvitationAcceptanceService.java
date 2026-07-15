package com.bp20.backend.api.auth.service;

import com.bp20.backend.api.auth.dto.request.AcceptStoreOwnerInvitationRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.iam.domain.IamLogAction;
import com.bp20.backend.api.invitation.domain.StoreOwnerInvitation;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import com.bp20.backend.api.invitation.repository.StoreOwnerInvitationRepository;
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
public class StoreOwnerInvitationAcceptanceService {

    private final UserRepository userRepository;
    private final StoreOwnerInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;
    private final JwtTokenProvider jwtTokenProvider;
    private final IamLogService iamLogService;

    @Transactional
    public LoginResponse acceptInvitation(AcceptStoreOwnerInvitationRequest request, String sourceIp) {
        String email = normalizeEmail(request.email());
        StoreOwnerInvitation invitation = invitationRepository
                .findByEmailAndTemporaryPasswordHashAndAcceptedAtIsNullAndRevokedAtIsNull(
                        email,
                        temporaryPasswordService.hash(request.temporaryPassword())
                )
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE_OWNER_INVITATION));

        LocalDateTime now = LocalDateTime.now();
        if (!invitation.isUsable(now)) {
            throw new ApiException(ErrorCode.CONFLICT_EXPIRED_STORE_OWNER_INVITATION);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }

        User storeOwner = userRepository.save(User.createStoreOwner(
                email,
                request.name().trim(),
                trimToNull(request.phoneNumber()),
                passwordEncoder.encode(request.newPassword())
        ));
        invitation.accept(now);
        iamLogService.record(invitation.getInvitedBy().getId(), IamLogAction.STORE_OWNER_INVITATION_ACCEPTED,
                storeOwner.getId(), storeOwner.getEmail(), sourceIp);

        return LoginResponse.of(jwtTokenProvider.createAccessToken(storeOwner), storeOwner);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
