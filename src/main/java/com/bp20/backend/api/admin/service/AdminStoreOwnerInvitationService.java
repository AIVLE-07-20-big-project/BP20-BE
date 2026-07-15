package com.bp20.backend.api.admin.service;

import com.bp20.backend.api.admin.dto.request.CreateStoreOwnerInvitationRequest;
import com.bp20.backend.api.admin.dto.response.StoreOwnerInvitationResponse;
import com.bp20.backend.api.iam.domain.IamLogAction;
import com.bp20.backend.api.invitation.domain.StoreOwnerInvitation;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.api.invitation.repository.StoreOwnerInvitationRepository;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.api.iam.service.IamLogService;
import com.bp20.backend.api.invitation.service.TemporaryPasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminStoreOwnerInvitationService {

    private final UserRepository userRepository;
    private final StoreOwnerInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;
    private final IamLogService iamLogService;

    @Value("${app.iam.invitation-expiration-hours:24}")
    private long invitationExpirationHours;

    @Transactional
    public StoreOwnerInvitationResponse createInvitation(
            Long actorUserId,
            CreateStoreOwnerInvitationRequest request,
            String sourceIp
    ) {
        User actor = requireAdminAndPassword(actorUserId, request.currentPassword());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findByEmailAndAcceptedAtIsNullAndRevokedAtIsNull(email)
                .forEach(invitation -> invitation.revoke(now));

        String temporaryPassword = temporaryPasswordService.generate();
        StoreOwnerInvitation invitation = invitationRepository.save(StoreOwnerInvitation.create(
                email,
                temporaryPasswordService.hash(temporaryPassword),
                actor,
                now.plusHours(invitationExpirationHours)
        ));
        iamLogService.record(actorUserId, IamLogAction.STORE_OWNER_INVITATION_CREATED,
                null, email, sourceIp);

        return StoreOwnerInvitationResponse.from(invitation, temporaryPassword);
    }

    private User requireAdminAndPassword(Long actorUserId, String currentPassword) {
        User actor = userRepository.findByIdWithPrivateInfo(actorUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (actor.getRole() != UserRole.SUPER_ADMIN && actor.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN_ADMIN_REQUIRED);
        }
        if (!passwordEncoder.matches(currentPassword, actor.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_PASSWORD);
        }
        return actor;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
