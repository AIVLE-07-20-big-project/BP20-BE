package com.bp20.backend.api.user.domain;

import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_status", columnList = "status")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @OneToOne(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            optional = false
    )
    @JoinColumn(name = "private_info_id", nullable = false, unique = true)
    private UserPrivateInfo privateInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    private User(String email, String name, String phoneNumber, String passwordHash, UserRole role) {
        this.privateInfo = UserPrivateInfo.of(email, passwordHash, name, phoneNumber);
        this.role = role;
        this.status = UserStatus.ACTIVE;
        initializeAccountSecurity(passwordHash);
    }

    private User(UserPrivateInfo privateInfo, UserRole role) {
        this.privateInfo = privateInfo;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        initializeAccountSecurity(privateInfo.getPasswordHash());
    }

    public static User createStoreOwner(String email, String name, String phoneNumber, String passwordHash) {
        return new User(email, name, phoneNumber, passwordHash, UserRole.STORE_OWNER);
    }

    public static User createStoreOwner(UserPrivateInfo privateInfo) {
        return new User(privateInfo, UserRole.STORE_OWNER);
    }

    public static User createAdmin(String email, String name, String phoneNumber, String passwordHash) {
        return new User(email, name, phoneNumber, passwordHash, UserRole.ADMIN);
    }

    public static User createAdmin(UserPrivateInfo privateInfo) {
        return new User(privateInfo, UserRole.ADMIN);
    }

    public static User createSuperAdmin(String email, String name, String phoneNumber, String passwordHash) {
        return new User(email, name, phoneNumber, passwordHash, UserRole.SUPER_ADMIN);
    }

    public static User createSuperAdmin(UserPrivateInfo privateInfo) {
        return new User(privateInfo, UserRole.SUPER_ADMIN);
    }

    public String getEmail() { return privateInfo.getEmail(); }
    public String getPasswordHash() { return privateInfo.getPasswordHash(); }
    public String getName() { return privateInfo.getName(); }
    public String getPhoneNumber() { return privateInfo.getPhoneNumber(); }

    public boolean isStoreOwner() {
        return this.role == UserRole.STORE_OWNER;
    }

    public boolean isSuperAdmin() {
        return this.role == UserRole.SUPER_ADMIN;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public void updateProfile(String name, String phoneNumber) {
        if (name != null && !name.isBlank()) {
            this.privateInfo.updateName(name);
        }
        if (phoneNumber != null) {
            this.privateInfo.updatePhoneNumber(phoneNumber);
        }
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public boolean isTemporarilyLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isPasswordExpired(LocalDateTime now, Duration maxAge) {
        return passwordChangedAt != null && passwordChangedAt.plus(maxAge).isBefore(now);
    }

    public void registerFailedLogin(int maxAttempts, Duration lockDuration, LocalDateTime now) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockDuration);
            this.failedLoginAttempts = 0;
        }
    }

    public void loginSucceeded() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void changePassword(String passwordHash, LocalDateTime now) {
        this.privateInfo.updatePassword(passwordHash);
        this.passwordChangedAt = now;
        loginSucceeded();
    }

    private void initializeAccountSecurity(String passwordHash) {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.passwordChangedAt = passwordHash == null ? null : LocalDateTime.now();
    }
}
