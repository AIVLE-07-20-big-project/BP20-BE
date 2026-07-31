package com.bp20.backend.api.csv.repository;

import com.bp20.backend.api.csv.domain.CsvDailySales;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface CsvDailySalesRepository extends JpaRepository<CsvDailySales, Long> {
    List<CsvDailySales> findAllByOwner_IdOrderBySaleDateAscProductCodeAsc(Long ownerId);
    long countByOwner_Id(Long ownerId);
    void deleteAllByOwner_Id(Long ownerId);
    List<CsvDailySales> findByOwner_IdAndSaleDateGreaterThanEqualAndSaleDateLessThan(
            Long ownerId, LocalDate from, LocalDate to
    );
    void deleteAllByOwner_IdAndSaleDateGreaterThanEqualAndSaleDateLessThan(
            Long ownerId, LocalDate from, LocalDate to
    );
}
