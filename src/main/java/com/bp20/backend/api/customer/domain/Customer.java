package com.bp20.backend.api.customer.domain;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.user.domain.UserPrivateInfo;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "customers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "private_info_id", nullable = false)
    private UserPrivateInfo privateInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerStatus status;

    private Customer(Store store, UserPrivateInfo privateInfo) {
        this.store = store;
        this.privateInfo = privateInfo;
        this.status = CustomerStatus.ACTIVE;
    }

    public static Customer create(Store store, UserPrivateInfo privateInfo) {
        return new Customer(store, privateInfo);
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    public String getEmail() {
        return privateInfo.getEmail();
    }

    public String getName() {
        return privateInfo.getName();
    }

    public String getPhoneNumber() {
        return privateInfo.getPhoneNumber();
    }
}
