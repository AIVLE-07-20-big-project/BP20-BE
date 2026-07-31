package com.bp20.backend.api.csv.repository;

import com.bp20.backend.api.csv.domain.CsvDailySales;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface CsvDailySalesRepository extends JpaRepository<CsvDailySales, Long> {
    List<CsvDailySales> findAllByOwnerIdOrderBySaleDateAscProductCodeAsc(Long ownerId);
    long countByOwnerId(Long ownerId);
    void deleteAllByOwnerId(Long ownerId);
    List<CsvDailySales> findByOwnerIdAndSaleDateGreaterThanEqualAndSaleDateLessThan(
            Long ownerId, LocalDate from, LocalDate to
    );
    void deleteAllByOwnerIdAndSaleDateGreaterThanEqualAndSaleDateLessThan(
            Long ownerId, LocalDate from, LocalDate to
    );
}
