package com.bp20.backend.api.location.domain;

import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
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
        name = "store_owner_locations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_store_owner_locations_owner",
                columnNames = "owner_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreOwnerLocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private StoreOwnerLocation(
            User owner,
            String displayName,
            double latitude,
            double longitude
    ) {
        this.owner = owner;
        update(displayName, latitude, longitude);
    }

    public static StoreOwnerLocation create(
            User owner,
            String displayName,
            double latitude,
            double longitude
    ) {
        return new StoreOwnerLocation(
                owner,
                displayName,
                latitude,
                longitude
        );
    }

    public void update(
            String displayName,
            double latitude,
            double longitude
    ) {
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
