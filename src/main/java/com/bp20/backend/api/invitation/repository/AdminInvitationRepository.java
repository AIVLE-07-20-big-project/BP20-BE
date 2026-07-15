package com.bp20.backend.api.invitation.repository;

import com.bp20.backend.api.invitation.domain.AdminInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminInvitationRepository extends JpaRepository<AdminInvitation, Long> {
    Optional<AdminInvitation> findByEmailAndTemporaryPasswordHashAndAcceptedAtIsNullAndRevokedAtIsNull(
            String email,
            String temporaryPasswordHash
    );
    List<AdminInvitation> findByEmailAndAcceptedAtIsNullAndRevokedAtIsNull(String email);
}
