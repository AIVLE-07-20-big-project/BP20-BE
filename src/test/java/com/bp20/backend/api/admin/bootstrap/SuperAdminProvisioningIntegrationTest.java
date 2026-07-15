package com.bp20.backend.api.admin.bootstrap;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SuperAdminProvisioningIntegrationTest {

    @Autowired
    private SuperAdminProvisioningService provisioningService;

    @Test
    void provisionsOnlyOneSuperAdministrator() {
        User superAdmin = provisioningService.provision(
                "Root.Admin@Example.com",
                "StrongPassw0rd!23",
                "Root Admin",
                null
        );

        assertThat(superAdmin.getEmail()).isEqualTo("root.admin@example.com");
        assertThat(superAdmin.getRole()).isEqualTo(UserRole.SUPER_ADMIN);

        assertThatThrownBy(() -> provisioningService.provision(
                "another-root@example.com",
                "StrongPassw0rd!24",
                "Another Root",
                null
        )).isInstanceOf(IllegalStateException.class);
    }
}
