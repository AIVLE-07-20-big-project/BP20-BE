package com.bp20.backend.api.iam.log.repository;

import com.bp20.backend.api.iam.log.domain.IamLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IamLogRepository extends JpaRepository<IamLog, Long> {
    @EntityGraph(attributePaths = {"actorUser", "targetUser"})
    List<IamLog> findTop100ByOrderByIdDesc();
}
