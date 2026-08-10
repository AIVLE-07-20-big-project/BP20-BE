package com.bp20.backend.api.iam.admin.service;

import com.bp20.backend.api.iam.admin.dto.response.AdminAccountResponse;
import com.bp20.backend.api.iam.admin.dto.response.AdminPersonalDataResponse;
import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.api.iam.log.service.IamLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final long PERSONAL_DATA_REVEAL_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamLogService iamLogService;

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAdmins() {
        return userRepository.findByRoleInOrderByIdDesc(List.of(UserRole.SUPER_ADMIN, UserRole.ADMIN)).stream()
                .map(AdminAccountResponse::from)
                .toList();
    }

    @Transactional
    public AdminAccountResponse deactivateAdmin(Long actorUserId, Long adminId, String currentPassword, String sourceIp) {
        requireSuperAdminAndPassword(actorUserId, currentPassword);
        User admin = userRepository.findByIdAndRole(adminId, UserRole.ADMIN)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        admin.deactivate();
        iamLogService.record(actorUserId, IamLogAction.ADMIN_DEACTIVATED,
                admin.getId(), admin.getEmail(), sourceIp);
        return AdminAccountResponse.from(admin);
    }

    @Transactional
    public AdminAccountResponse activateAdmin(Long actorUserId, Long adminId, String currentPassword, String sourceIp) {
        requireSuperAdminAndPassword(actorUserId, currentPassword);
        User admin = userRepository.findByIdAndRole(adminId, UserRole.ADMIN)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        admin.activate();
        iamLogService.record(actorUserId, IamLogAction.ADMIN_ACTIVATED,
                admin.getId(), admin.getEmail(), sourceIp);
        return AdminAccountResponse.from(admin);
    }

    @Transactional
    public AdminPersonalDataResponse revealPersonalData(
            Long actorUserId,
            Long adminId,
            String currentPassword,
            String sourceIp
    ) {
        User actor = requireSuperAdmin(actorUserId);
        User admin = userRepository.findByIdAndRole(adminId, UserRole.ADMIN)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));

        if (!passwordEncoder.matches(currentPassword, actor.getPasswordHash())) {
            iamLogService.recordInNewTransaction(
                    actorUserId,
                    IamLogAction.ADMIN_PERSONAL_DATA_REVEAL_FAILED,
                    admin.getId(),
                    admin.getEmail(),
                    sourceIp
            );
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_PASSWORD);
        }

        iamLogService.record(
                actorUserId,
                IamLogAction.ADMIN_PERSONAL_DATA_REVEALED,
                admin.getId(),
                admin.getEmail(),
                sourceIp
        );
        return AdminPersonalDataResponse.from(
                admin,
                Instant.now().plus(PERSONAL_DATA_REVEAL_SECONDS, ChronoUnit.SECONDS)
        );
    }

    private void requireSuperAdminAndPassword(Long actorUserId, String currentPassword) {
        User actor = requireSuperAdmin(actorUserId);
        if (!passwordEncoder.matches(currentPassword, actor.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_PASSWORD);
        }
    }

    private User requireSuperAdmin(Long actorUserId) {
        User actor = userRepository.findByIdWithPrivateInfo(actorUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (!actor.isSuperAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN_SUPER_ADMIN_REQUIRED);
        }
        return actor;
    }
}
