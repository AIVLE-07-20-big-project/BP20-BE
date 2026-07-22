package com.bp20.backend.global.security.principal;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;

public record SecurityPrincipal(
        Long id,
        UserRole role
) {
    public static SecurityPrincipal from(User user) {
        return new SecurityPrincipal(user.getId(), user.getRole());
    }
}
