package com.bp20.backend.api.admin.service;

import com.bp20.backend.api.admin.dto.request.CreateAdminInvitationRequest;
import com.bp20.backend.api.admin.dto.response.AdminInvitationResponse;
import com.bp20.backend.api.invitation.domain.AdminInvitation;
import com.bp20.backend.api.iam.domain.IamLogAction;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.api.invitation.repository.AdminInvitationRepository;
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
public class AdminInvitationService {

    private final UserRepository userRepository;
    private final AdminInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamLogService iamLogService;
    private final TemporaryPasswordService temporaryPasswordService;

    @Value("${app.iam.invitation-expiration-hours:24}")
    private long invitationExpirationHours;

    @Transactional
    public AdminInvitationResponse createInvitation(
            Long actorUserId,
            CreateAdminInvitationRequest request,
            String sourceIp
    ) {
        User actor = requireSuperAdminAndPassword(actorUserId, request.currentPassword());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findByEmailAndAcceptedAtIsNullAndRevokedAtIsNull(email)
                .forEach(invitation -> invitation.revoke(now));

        String temporaryPassword = temporaryPasswordService.generate();
        AdminInvitation invitation = invitationRepository.save(AdminInvitation.create(
                email,
                temporaryPasswordService.hash(temporaryPassword),
                actor,
                now.plusHours(invitationExpirationHours)
        ));
        iamLogService.record(actorUserId, IamLogAction.ADMIN_INVITATION_CREATED,
                null, email, sourceIp);

        return AdminInvitationResponse.from(invitation, temporaryPassword);
    }

    private User requireSuperAdminAndPassword(Long actorUserId, String currentPassword) {
        User actor = userRepository.findByIdWithPrivateInfo(actorUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (!actor.isSuperAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN_SUPER_ADMIN_REQUIRED);
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
