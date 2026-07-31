package com.bp20.backend.api.location.repository;

import com.bp20.backend.api.location.domain.StoreOwnerLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoreOwnerLocationRepository
        extends JpaRepository<StoreOwnerLocation, Long> {

    Optional<StoreOwnerLocation> findByOwner_Id(Long ownerId);
}
