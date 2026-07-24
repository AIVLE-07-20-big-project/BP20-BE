package com.bp20.backend.api.auth;

import com.bp20.backend.api.auth.dto.request.LoginRequest;
import com.bp20.backend.api.auth.dto.request.SignupRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.dto.response.MeResponse;
import com.bp20.backend.api.auth.dto.response.SignupResponse;
import com.bp20.backend.api.auth.service.LoginService;
import com.bp20.backend.api.auth.service.SignupService;
import com.bp20.backend.api.iam.invitation.dto.request.InvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationResponse;
import com.bp20.backend.api.iam.invitation.service.InvitationService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserPrivateInfo;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.repository.UserPrivateInfoRepository;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AuthIntegrationTest {

    private static final String PASSWORD = "Passw0rd!234";

    @Autowired
    private LoginService loginService;

    @Autowired
    private SignupService signupService;

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPrivateInfoRepository userPrivateInfoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void storeOwnerCanSignupAndLoginWithInvitation() {
        signupStoreOwner("auth.owner@example.com");

        LoginResponse loginResponse = loginService.login(new LoginRequest(
                "Auth.Owner@Example.com",
                PASSWORD
        ));
        MeResponse me = loginService.getMe(
                jwtTokenProvider.extractUserId(loginResponse.accessToken())
        );

        assertThat(loginResponse.accessToken()).isNotBlank();
        String payload = new String(
                Base64.getUrlDecoder().decode(loginResponse.accessToken().split("\\.")[1]),
                StandardCharsets.UTF_8
        );
        assertThat(payload).doesNotContain("email", "role");
        assertThat(me.email()).isEqualTo("auth.owner@example.com");
        assertThat(me.role()).isEqualTo(UserRole.STORE_OWNER);
    }

    @Test
    void signupRoleIsDeterminedByAdminInvitation() {
        User superAdmin = userRepository.save(User.createSuperAdmin(
                "signup-super-admin@example.com",
                "Super Admin",
                null,
                passwordEncoder.encode(PASSWORD)
        ));
        InvitationResponse invitation = invitationService.inviteAdmin(
                superAdmin.getId(),
                new InvitationRequest("new-admin@example.com", PASSWORD),
                "127.0.0.1"
        );

        SignupResponse signupResponse = signupService.signup(
                new SignupRequest(
                        "new-admin@example.com",
                        invitation.temporaryPassword(),
                        PASSWORD,
                        "New Admin",
                        null
                ),
                "127.0.0.1"
        );

        assertThat(signupResponse.role()).isEqualTo(UserRole.ADMIN);
        assertThat(signupResponse.accessToken()).isNotBlank();
    }

    @Test
    void loginFailsWithWrongPassword() {
        signupStoreOwner("wrong-password@example.com");

        assertThatThrownBy(() -> loginService.login(new LoginRequest(
                "wrong-password@example.com",
                "wrong-password"
        ))).isInstanceOf(ApiException.class);
    }

    @Test
    void customerPrivateInfoIsReusedWhenCustomerAcceptsStoreOwnerInvitation() {
        UserPrivateInfo customerPrivateInfo = userPrivateInfoRepository.save(
                UserPrivateInfo.forCustomer(
                        "customer-signup@example.com",
                        "Customer",
                        "010-1111-2222"
                )
        );
        User inviter = userRepository.save(User.createAdmin(
                "customer-signup-inviter@example.com",
                "Auth Inviter",
                null,
                passwordEncoder.encode(PASSWORD)
        ));
        InvitationResponse invitation = invitationService.inviteStoreOwner(
                inviter.getId(),
                new InvitationRequest("customer-signup@example.com", PASSWORD),
                "127.0.0.1"
        );

        signupService.signup(
                new SignupRequest(
                        "customer-signup@example.com",
                        invitation.temporaryPassword(),
                        PASSWORD,
                        "Store Owner",
                        "010-9999-8888"
                ),
                "127.0.0.1"
        );

        User signedUpUser = userRepository.findByEmail("customer-signup@example.com").orElseThrow();
        assertThat(signedUpUser.getPrivateInfo().getId()).isEqualTo(customerPrivateInfo.getId());
        assertThat(passwordEncoder.matches(PASSWORD, signedUpUser.getPasswordHash())).isTrue();
        assertThat(signedUpUser.getName()).isEqualTo("Store Owner");
    }

    private void signupStoreOwner(String email) {
        User inviter = userRepository.save(User.createAdmin(
                "inviter-" + email,
                "Auth Inviter",
                null,
                passwordEncoder.encode(PASSWORD)
        ));
        InvitationResponse invitation = invitationService.inviteStoreOwner(
                inviter.getId(),
                new InvitationRequest(email, PASSWORD),
                "127.0.0.1"
        );
        signupService.signup(
                new SignupRequest(
                        email,
                        invitation.temporaryPassword(),
                        PASSWORD,
                        "Auth Owner",
                        null
                ),
                "127.0.0.1"
        );
    }
}
