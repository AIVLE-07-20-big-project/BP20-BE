package com.bp20.backend.api.csv.repository;

import com.bp20.backend.api.csv.domain.CsvDailySales;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CsvDailySalesRepository extends JpaRepository<CsvDailySales, Long> {
    List<CsvDailySales> findAllByOwner_IdOrderBySaleDateAscProductCodeAsc(Long ownerId);
    long countByOwner_Id(Long ownerId);
    void deleteAllByOwner_Id(Long ownerId);
}
