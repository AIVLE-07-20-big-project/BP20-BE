package com.bp20.backend.api.iam.admin.bootstrap;

import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.api.iam.log.service.IamLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SuperAdminProvisioningService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IamLogService iamLogService;

    @Transactional
    public User provision(String email, String password, String name, String phoneNumber) {
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            throw new IllegalStateException("A super administrator already exists.");
        }
        if (password == null || password.length() < 12 || password.length() > 72) {
            throw new IllegalArgumentException("The super administrator password must be 12 to 72 characters.");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("The email is already in use.");
        }

        User superAdmin = userRepository.save(User.createSuperAdmin(
                normalizedEmail,
                name.trim(),
                trimToNull(phoneNumber),
                passwordEncoder.encode(password)
        ));
        iamLogService.record(null, IamLogAction.SUPER_ADMIN_CREATED,
                superAdmin.getId(), superAdmin.getEmail(), "CLI");
        return superAdmin;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
