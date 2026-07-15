package com.bp20.backend.api.admin.service;

import com.bp20.backend.api.admin.dto.response.AdminAccountResponse;
import com.bp20.backend.api.iam.domain.IamLogAction;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.api.iam.service.IamLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccountService {

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

    private void requireSuperAdminAndPassword(Long actorUserId, String currentPassword) {
        User actor = userRepository.findByIdWithPrivateInfo(actorUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
        if (!actor.isSuperAdmin()) {
            throw new ApiException(ErrorCode.FORBIDDEN_SUPER_ADMIN_REQUIRED);
        }
        if (!passwordEncoder.matches(currentPassword, actor.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED_INVALID_PASSWORD);
        }
    }
}
