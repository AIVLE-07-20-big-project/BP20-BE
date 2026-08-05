package com.bp20.backend.api.user.domain;

import com.bp20.backend.global.security.crypto.EncryptedStringConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.bp20.backend.global.util.PhoneNumberUtils.normalize;

@Getter
@Entity
@Table(
        name = "user_private_info",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_private_info_email", columnNames = "email")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPrivateInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "private_info_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 255)
    private String passwordHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 512)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 512)
    private String phoneNumber;

    private UserPrivateInfo(String email, String passwordHash, String name, String phoneNumber) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phoneNumber = normalize(phoneNumber);
    }

    public static UserPrivateInfo of(String email, String passwordHash, String name, String phoneNumber) {
        return new UserPrivateInfo(email, passwordHash, name, phoneNumber);
    }

    public static UserPrivateInfo forCustomer(String email, String name, String phoneNumber) {
        return new UserPrivateInfo(email, null, name, phoneNumber);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public void attachUserAccount(String passwordHash, String name, String phoneNumber) {
        this.passwordHash = passwordHash;
        this.name = name;
        this.phoneNumber = normalize(phoneNumber);
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = normalize(phoneNumber);
    }

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
