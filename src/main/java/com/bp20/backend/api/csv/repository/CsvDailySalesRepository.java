package com.bp20.backend.api.csv.repository;

import com.bp20.backend.api.csv.domain.CsvDailySales;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface CsvDailySalesRepository extends JpaRepository<CsvDailySales, Long> {
    List<CsvDailySales> findAllByOwnerIdOrderBySaleDateAscProductCodeAsc(Long ownerId);
    long countByOwnerId(Long ownerId);
    void deleteAllByOwnerId(Long ownerId);

    // 신규 가맹점 영업 타겟 추천 - 우수 가맹점 판별용 매출 성장률 계산에 사용.
    // from(포함) ~ to(미포함) 구간의 매출 합계. 매칭되는 행이 없으면 0을 반환한다(coalesce).
    @Query("select coalesce(sum(c.salesAmount), 0) from CsvDailySales c "
            + "where c.ownerId = :ownerId and c.saleDate >= :from and c.saleDate < :to")
    long sumSalesAmountBetween(@Param("ownerId") Long ownerId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
