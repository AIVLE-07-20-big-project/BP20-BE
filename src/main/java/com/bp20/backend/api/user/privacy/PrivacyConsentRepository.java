package com.bp20.backend.api.user.privacy;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, Long> {
}
