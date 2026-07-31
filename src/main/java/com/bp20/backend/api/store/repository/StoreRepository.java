package com.bp20.backend.api.store.repository;

import com.bp20.backend.api.store.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByOwnerId(Long ownerId);

    Optional<Store> findByIdAndOwnerId(Long id, Long ownerId);

    List<Store> findByOwnerIdIn(Collection<Long> ownerIds);

    boolean existsByOwnerId(Long ownerId);

    boolean existsByBusinessNumber(String businessNumber);
}
