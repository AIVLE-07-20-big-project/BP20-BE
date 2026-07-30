package com.bp20.backend.api.iam.invitation.service;

import com.bp20.backend.api.iam.invitation.domain.Invitation;
import com.bp20.backend.api.iam.invitation.dto.request.InvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationSummaryResponse;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationResponse;
import com.bp20.backend.api.iam.invitation.repository.InvitationRepository;
import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.iam.log.service.IamLogService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final UserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordService temporaryPasswordService;
    private final IamLogService iamLogService;

    @Value("${app.iam.invitation-expiration-hours:24}")
    private long invitationExpirationHours;

    @Transactional
    public InvitationResponse inviteAdmin(
            Long actorUserId,
            InvitationRequest request,
            String sourceIp
    ) {
        User actor = requireActorAndPassword(actorUserId, request.currentPassword());
        if (!actor.isSuperAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN_SUPER_ADMIN_REQUIRED);
        }
        return createInvitation(actor, UserRole.ADMIN, request.email(), sourceIp);
    }

    @Transactional
    public InvitationResponse inviteStoreOwner(
            Long actorUserId,
            InvitationRequest request,
            String sourceIp
    ) {
        User actor = requireActorAndPassword(actorUserId, request.currentPassword());
        if (actor.getRole() != UserRole.SUPER_ADMIN && actor.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN_ADMIN_REQUIRED);
        }
        return createInvitation(actor, UserRole.STORE_OWNER, request.email(), sourceIp);
    }

    @Transactional(readOnly = true)
    public List<InvitationSummaryResponse> getInvitations(UserRole actorRole) {
        List<UserRole> visibleRoles = actorRole == UserRole.SUPER_ADMIN
                ? List.of(UserRole.ADMIN, UserRole.STORE_OWNER)
                : List.of(UserRole.STORE_OWNER);
        LocalDateTime now = LocalDateTime.now();

        return invitationRepository.findTop100ByTargetRoleInOrderByIdDesc(visibleRoles).stream()
                .map(invitation -> InvitationSummaryResponse.from(invitation, now))
                .toList();
    }

    @Transactional
    public InvitationSummaryResponse revokeInvitation(
            Long actorUserId,
            Long invitationId,
            String currentPassword,
            String sourceIp
    ) {
        User actor = requireActorAndPassword(actorUserId, currentPassword);
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_INVITATION));

        if (actor.getRole() == UserRole.ADMIN && invitation.getTargetRole() != UserRole.STORE_OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN_SUPER_ADMIN_REQUIRED);
        }
        if (actor.getRole() != UserRole.SUPER_ADMIN && actor.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN_ADMIN_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!invitation.isUsable(now)) {
            throw new ApiException(ErrorCode.CONFLICT_INVITATION_NOT_REVOCABLE);
        }

        invitation.revoke(now);
        iamLogService.record(
                actor.getId(),
                invitationRevokedAction(invitation.getTargetRole()),
                null,
                invitation.getEmail(),
                sourceIp
        );
        return InvitationSummaryResponse.from(invitation, now);
    }

    private InvitationResponse createInvitation(
            User actor,
            UserRole targetRole,
            String requestedEmail,
            String sourceIp
    ) {
        String email = normalizeEmail(requestedEmail);
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.CONFLICT_DUPLICATE_EMAIL);
        }

        LocalDateTime now = LocalDateTime.now();
        invitationRepository.findByEmailAndTargetRoleAndAcceptedAtIsNullAndRevokedAtIsNull(email, targetRole)
                .forEach(invitation -> invitation.revoke(now));

        String temporaryPassword = temporaryPasswordService.generate();
        Invitation invitation = invitationRepository.save(Invitation.create(
                email,
                targetRole,
                temporaryPasswordService.hash(temporaryPassword),
                actor,
                now.plusHours(invitationExpirationHours)
        ));
        iamLogService.record(
                actor.getId(),
                invitationCreatedAction(targetRole),
                null,
                email,
                sourceIp
        );

        return InvitationResponse.from(invitation, temporaryPassword);
    }

    private User requireActorAndPassword(Long actorUserId, String currentPassword) {
        User actor = userRepository.findByIdWithPrivateInfo(actorUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (!passwordEncoder.matches(currentPassword, actor.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_PASSWORD);
        }
        return actor;
    }

    private IamLogAction invitationCreatedAction(UserRole targetRole) {
        return targetRole == UserRole.ADMIN
                ? IamLogAction.ADMIN_INVITATION_CREATED
                : IamLogAction.STORE_OWNER_INVITATION_CREATED;
    }

    private IamLogAction invitationRevokedAction(UserRole targetRole) {
        return targetRole == UserRole.ADMIN
                ? IamLogAction.ADMIN_INVITATION_REVOKED
                : IamLogAction.STORE_OWNER_INVITATION_REVOKED;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
