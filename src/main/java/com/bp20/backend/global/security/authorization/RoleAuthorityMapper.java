package com.bp20.backend.global.security.authorization;

import com.bp20.backend.api.user.domain.UserRole;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class RoleAuthorityMapper {

    private static final Map<UserRole, List<Permission>> ROLE_PERMISSIONS = Map.of(
            UserRole.SUPER_ADMIN, List.of(
                    Permission.IAM_ADMIN_MANAGE,
                    Permission.ADMIN_MANAGE
            ),
            UserRole.ADMIN, List.of(Permission.ADMIN_MANAGE),
            UserRole.STORE_OWNER, List.of(Permission.STORE_OWNER_ACCESS)
    );

    public List<SimpleGrantedAuthority> getAuthorities(UserRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, List.of()).stream()
                .map(Enum::name)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
