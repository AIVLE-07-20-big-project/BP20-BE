package com.bp20.backend.api.location.domain;

import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    private StoreOwnerLocation(
            Long ownerId,
            String displayName,
            double latitude,
            double longitude
    ) {
        this.ownerId = ownerId;
        update(displayName, latitude, longitude);
    }

    public static StoreOwnerLocation create(
            Long ownerId,
            String displayName,
            double latitude,
            double longitude
    ) {
        return new StoreOwnerLocation(
                ownerId,
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
