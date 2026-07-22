package com.bp20.backend.api.user.repository;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select (count(u) > 0) from User u where u.privateInfo.email = :email")
    boolean existsByEmail(String email);

    @Query("select u from User u join fetch u.privateInfo where u.privateInfo.email = :email")
    Optional<User> findByEmail(String email);

    List<User> findByRoleOrderByIdDesc(UserRole role);

    List<User> findByRoleInOrderByIdDesc(Collection<UserRole> roles);

    Optional<User> findByIdAndRole(Long id, UserRole role);

    boolean existsByRole(UserRole role);

    @Query("select u from User u join fetch u.privateInfo where u.id = :id")
    Optional<User> findByIdWithPrivateInfo(Long id);
}
