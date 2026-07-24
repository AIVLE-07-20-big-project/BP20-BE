package com.bp20.backend.api.user.repository;

import com.bp20.backend.api.user.domain.UserPrivateInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPrivateInfoRepository extends JpaRepository<UserPrivateInfo, Long> {

    Optional<UserPrivateInfo> findByEmailIgnoreCase(String email);
}
