package com.bp20.backend.api.iam.invitation.repository;

import com.bp20.backend.api.iam.invitation.domain.Invitation;
import com.bp20.backend.api.user.domain.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByEmailAndTemporaryPasswordHashAndAcceptedAtIsNullAndRevokedAtIsNull(
            String email,
            String temporaryPasswordHash
    );

    List<Invitation> findByEmailAndTargetRoleAndAcceptedAtIsNullAndRevokedAtIsNull(
            String email,
            UserRole targetRole
    );

    @EntityGraph(attributePaths = {"invitedBy", "invitedBy.privateInfo"})
    List<Invitation> findTop100ByTargetRoleInOrderByIdDesc(Collection<UserRole> targetRoles);
}
