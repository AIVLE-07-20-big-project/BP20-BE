package com.bp20.backend.csv.repository;

import com.bp20.backend.csv.entity.CsvProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CsvProductRepository extends JpaRepository<CsvProduct, Long> {
    List<CsvProduct> findAllByOwnerIdOrderByProductCode(Long ownerId);
    long countByOwnerId(Long ownerId);
    void deleteAllByOwnerId(Long ownerId);
}
