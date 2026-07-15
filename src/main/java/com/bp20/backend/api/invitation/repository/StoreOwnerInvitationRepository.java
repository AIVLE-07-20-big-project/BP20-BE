package com.bp20.backend.api.invitation.repository;

import com.bp20.backend.api.invitation.domain.StoreOwnerInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreOwnerInvitationRepository extends JpaRepository<StoreOwnerInvitation, Long> {

    Optional<StoreOwnerInvitation> findByEmailAndTemporaryPasswordHashAndAcceptedAtIsNullAndRevokedAtIsNull(
            String email,
            String temporaryPasswordHash
    );

    List<StoreOwnerInvitation> findByEmailAndAcceptedAtIsNullAndRevokedAtIsNull(String email);
}
