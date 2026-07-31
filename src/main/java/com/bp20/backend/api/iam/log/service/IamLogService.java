package com.bp20.backend.api.iam.log.service;

import com.bp20.backend.api.iam.log.domain.IamLog;
import com.bp20.backend.api.iam.log.domain.IamLogAction;
import com.bp20.backend.api.iam.log.repository.IamLogRepository;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IamLogService {

    private final IamLogRepository iamLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(Long actorUserId, IamLogAction action, Long targetUserId,
                       String targetEmail, String sourceIp) {
        User actorUser = findOptionalUser(actorUserId);
        User targetUser = findOptionalUser(targetUserId);
        iamLogRepository.save(IamLog.of(
                actorUser,
                action,
                targetUser,
                targetEmail,
                sanitizeSourceIp(sourceIp)
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

    private User findOptionalUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_USER));
    }
}
