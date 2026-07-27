package com.bp20.backend.csv.repository;

import com.bp20.backend.csv.entity.CsvInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CsvInventoryRepository extends JpaRepository<CsvInventory, Long> {
    List<CsvInventory> findAllByOwnerIdOrderByName(Long ownerId);
    long countByOwnerId(Long ownerId);
    void deleteAllByOwnerId(Long ownerId);
}
