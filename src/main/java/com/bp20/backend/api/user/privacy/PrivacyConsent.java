package com.bp20.backend.api.user.privacy;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.security.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "privacy_consents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_privacy_consents_user_version",
                columnNames = {"user_id", "policy_version"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrivacyConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "privacy_consent_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "source_ip", length = 512)
    private String sourceIp;

    private PrivacyConsent(User user, String policyVersion, LocalDateTime agreedAt, String sourceIp) {
        this.user = user;
        this.policyVersion = policyVersion;
        this.agreedAt = agreedAt;
        this.sourceIp = sourceIp;
    }

    public static PrivacyConsent agreed(User user, String policyVersion, String sourceIp) {
        return new PrivacyConsent(user, policyVersion, LocalDateTime.now(), sanitizeIp(sourceIp));
    }

    private static String sanitizeIp(String sourceIp) {
        if (sourceIp == null || sourceIp.isBlank()) {
            return null;
        }
        return sourceIp.length() <= 45 ? sourceIp : sourceIp.substring(0, 45);
    }
}
