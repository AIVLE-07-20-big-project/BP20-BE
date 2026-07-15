package com.bp20.backend.api.iam.repository;

import com.bp20.backend.api.iam.domain.IamLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IamLogRepository extends JpaRepository<IamLog, Long> {
    List<IamLog> findTop100ByOrderByIdDesc();
}
