package com.bp20.backend.api.iam;

import com.bp20.backend.api.iam.invitation.dto.request.InvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationResponse;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationSummaryResponse;
import com.bp20.backend.api.iam.invitation.service.InvitationService;
import com.bp20.backend.api.iam.storeowner.dto.response.StoreOwnerAccountResponse;
import com.bp20.backend.api.iam.storeowner.service.StoreOwnerAccountService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.domain.UserStatus;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AccountManagementIntegrationTest {

    private static final String PASSWORD = "Passw0rd!234";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private StoreOwnerAccountService storeOwnerAccountService;

    @Test
    void superAdminSeesAllInvitationsAndAdminSeesOnlyStoreOwnerInvitations() {
        User superAdmin = saveSuperAdmin("account-super@example.com");
        User admin = saveAdmin("account-admin@example.com");

        invitationService.inviteAdmin(
                superAdmin.getId(),
                new InvitationRequest("invited-admin@example.com", PASSWORD),
                "127.0.0.1"
        );
        invitationService.inviteStoreOwner(
                admin.getId(),
                new InvitationRequest("invited-owner@example.com", PASSWORD),
                "127.0.0.1"
        );

        List<InvitationSummaryResponse> superAdminView =
                invitationService.getInvitations(UserRole.SUPER_ADMIN);
        List<InvitationSummaryResponse> adminView =
                invitationService.getInvitations(UserRole.ADMIN);

        assertThat(superAdminView)
                .extracting(InvitationSummaryResponse::targetRole)
                .contains(UserRole.ADMIN, UserRole.STORE_OWNER);
        assertThat(adminView)
                .extracting(InvitationSummaryResponse::targetRole)
                .containsOnly(UserRole.STORE_OWNER);
    }

    @Test
    void adminCannotRevokeAdminInvitation() {
        User superAdmin = saveSuperAdmin("revoke-super@example.com");
        User admin = saveAdmin("revoke-admin@example.com");
        InvitationResponse invitation = invitationService.inviteAdmin(
                superAdmin.getId(),
                new InvitationRequest("protected-admin@example.com", PASSWORD),
                "127.0.0.1"
        );

        assertThatThrownBy(() -> invitationService.revokeInvitation(
                admin.getId(),
                invitation.id(),
                PASSWORD,
                "127.0.0.1"
        ))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN_SUPER_ADMIN_REQUIRED);
    }

    @Test
    void adminCanManageStoreOwnerAccountStatus() {
        User admin = saveAdmin("status-admin@example.com");
        User owner = userRepository.save(User.createStoreOwner(
                "status-owner@example.com",
                "Store Owner",
                "010-1111-2222",
                passwordEncoder.encode(PASSWORD)
        ));

        StoreOwnerAccountResponse deactivated = storeOwnerAccountService.deactivate(
                admin.getId(),
                owner.getId(),
                PASSWORD,
                "127.0.0.1"
        );
        StoreOwnerAccountResponse activated = storeOwnerAccountService.activate(
                admin.getId(),
                owner.getId(),
                PASSWORD,
                "127.0.0.1"
        );

        assertThat(deactivated.status()).isEqualTo(UserStatus.INACTIVE);
        assertThat(activated.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(storeOwnerAccountService.getStoreOwners())
                .extracting(StoreOwnerAccountResponse::email)
                .contains("status-owner@example.com");
    }

    private User saveSuperAdmin(String email) {
        return userRepository.save(User.createSuperAdmin(
                email,
                "Super Admin",
                null,
                passwordEncoder.encode(PASSWORD)
        ));
    }

    private User saveAdmin(String email) {
        return userRepository.save(User.createAdmin(
                email,
                "Admin",
                null,
                passwordEncoder.encode(PASSWORD)
        ));
    }
}
