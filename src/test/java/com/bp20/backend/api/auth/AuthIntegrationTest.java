package com.bp20.backend.api.auth;

import com.bp20.backend.api.admin.dto.request.CreateStoreOwnerInvitationRequest;
import com.bp20.backend.api.admin.dto.response.StoreOwnerInvitationResponse;
import com.bp20.backend.api.admin.service.AdminStoreOwnerInvitationService;
import com.bp20.backend.api.auth.dto.request.LoginRequest;
import com.bp20.backend.api.auth.dto.request.AcceptStoreOwnerInvitationRequest;
import com.bp20.backend.api.auth.dto.response.LoginResponse;
import com.bp20.backend.api.auth.dto.response.MeResponse;
import com.bp20.backend.api.auth.service.AuthService;
import com.bp20.backend.api.auth.service.StoreOwnerInvitationAcceptanceService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.security.jwt.JwtTokenProvider;
import com.bp20.backend.global.exception.ApiException;
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

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminStoreOwnerInvitationService invitationService;

    @Autowired
    private StoreOwnerInvitationAcceptanceService invitationAcceptanceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void storeOwnerCanLoginAndTokenContainsOnlyStandardClaims() {
        createStoreOwner("Auth.Owner@Example.com", "123-45-67890");

        LoginResponse loginResponse = authService.login(new LoginRequest(
                "Auth.Owner@Example.com",
                "Passw0rd!23"
        ));
        MeResponse me = authService.getMe(
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
    void loginFailsWithWrongPassword() {
        createStoreOwner("wrong-password@example.com", "987-65-43210");

        assertThatThrownBy(() -> authService.login(new LoginRequest(
                "wrong-password@example.com",
                "wrong-password"
        ))).isInstanceOf(ApiException.class);
    }

    private void createStoreOwner(String email, String uniqueSuffix) {
        String inviterEmail = "auth-inviter-" + uniqueSuffix.replace("-", "") + "@example.com";
        User inviter = userRepository.save(User.createAdmin(
                inviterEmail,
                "Auth Inviter",
                null,
                passwordEncoder.encode("Passw0rd!23")
        ));
        StoreOwnerInvitationResponse invitation = invitationService.createInvitation(
                inviter.getId(),
                new CreateStoreOwnerInvitationRequest(email, "Passw0rd!23"),
                "127.0.0.1"
        );
        invitationAcceptanceService.acceptInvitation(
                new AcceptStoreOwnerInvitationRequest(
                        email,
                        invitation.temporaryPassword(),
                        "Passw0rd!23",
                        "Auth Owner",
                        null
                ),
                "127.0.0.1"
        );
    }
}
