package com.bp20.backend.api.csv.repository;

import com.bp20.backend.api.csv.domain.CsvProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CsvProductRepository extends JpaRepository<CsvProduct, Long> {
    List<CsvProduct> findAllByOwner_IdOrderByProductCode(Long ownerId);
    long countByOwner_Id(Long ownerId);
    void deleteAllByOwner_Id(Long ownerId);
}
