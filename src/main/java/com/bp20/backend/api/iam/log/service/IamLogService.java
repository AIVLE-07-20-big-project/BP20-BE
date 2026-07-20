package com.bp20.backend.api.iam.log.service;

import com.bp20.backend.api.iam.log.domain.IamLog;
import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.iam.log.repository.IamLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IamLogService {

    private final IamLogRepository iamLogRepository;

    @Transactional
    public void record(Long actorUserId, IamLogAction action, Long targetUserId,
                       String targetEmail, String sourceIp) {
        iamLogRepository.save(IamLog.of(
                actorUserId, action, targetUserId, targetEmail, sanitizeSourceIp(sourceIp)
        ));
    }

    @Transactional(readOnly = true)
    public List<IamLog> getRecentLogs() {
        return iamLogRepository.findTop100ByOrderByIdDesc();
    }

    private String sanitizeSourceIp(String sourceIp) {
        if (sourceIp == null || sourceIp.isBlank()) {
            return "unknown";
        }
        return sourceIp.length() <= 45 ? sourceIp : sourceIp.substring(0, 45);
    }
}
