package com.bp20.backend.api.store.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "stores",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stores_owner", columnNames = "owner_user_id"),
                @UniqueConstraint(name = "uk_stores_business_number", columnNames = "business_number")
        },
        indexes = @Index(name = "idx_stores_name", columnList = "name")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "business_number", nullable = false, length = 10)
    private String businessNumber;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_sales_status", nullable = false, length = 20)
    private OnlineSalesStatus onlineSalesStatus;

    private Store(
            User owner,
            String name,
            String businessNumber,
            String category,
            String address,
            String phoneNumber
    ) {
        this.owner = owner;
        this.name = name;
        this.businessNumber = businessNumber;
        this.category = category;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.onlineSalesStatus = OnlineSalesStatus.CLOSED;
    }

    public static Store create(
            User owner,
            String name,
            String businessNumber,
            String category,
            String address,
            String phoneNumber
    ) {
        return new Store(owner, name, businessNumber, category, address, phoneNumber);
    }

    public void update(String name, String category, String address, String phoneNumber) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

    public void changeOnlineSalesStatus(OnlineSalesStatus onlineSalesStatus) {
        this.onlineSalesStatus = onlineSalesStatus;
    }
}
