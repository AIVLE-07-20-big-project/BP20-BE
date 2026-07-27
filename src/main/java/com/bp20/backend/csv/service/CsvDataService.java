package com.bp20.backend.csv;

import com.bp20.backend.csv.entity.CsvDailySales;
import com.bp20.backend.csv.entity.CsvInventory;
import com.bp20.backend.csv.entity.CsvProduct;
import com.bp20.backend.csv.repository.CsvDailySalesRepository;
import com.bp20.backend.csv.repository.CsvInventoryRepository;
import com.bp20.backend.csv.repository.CsvProductRepository;
import com.bp20.backend.inventory.InventoryDataRequest;
import com.bp20.backend.product.ProductDataRequest;
import com.bp20.backend.sales.DailySalesDto;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CsvDataService {

    private final CsvProductRepository productRepository;
    private final CsvDailySalesRepository salesRepository;
    private final CsvInventoryRepository inventoryRepository;

    /**
     * 상품 CSV 파일을 읽어 로그인한 점주의 데이터로 교체 저장한다.
     */
    @Transactional
    public int loadProducts(Long ownerId, MultipartFile file) {
        List<ProductDataRequest> result = new ArrayList<>();

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            validateHeaders(
                    parser,
                    "product_code",
                    "product_name",
                    "ingredient1",
                    "ingredient2",
                    "ingredient3",
                    "ingredient4",
                    "ingredient5"
            );

            for (CSVRecord record : parser) {
                ProductDataRequest product = new ProductDataRequest(
                        record.get("product_code"),
                        record.get("product_name"),
                        getNullable(record, "ingredient1"),
                        getNullable(record, "ingredient2"),
                        getNullable(record, "ingredient3"),
                        getNullable(record, "ingredient4"),
                        getNullable(record, "ingredient5")
                );

                result.add(product);
            }

            /*
             * CSV 파일 전체를 정상적으로 읽은 후
             * 기존 상품 데이터를 새 데이터로 교체한다.
             *
             * 파일 처리 도중 오류가 발생하면 기존 데이터가 유지된다.
             */
            productRepository.deleteAllByOwnerId(ownerId);
            productRepository.flush();
            productRepository.saveAll(
                    result.stream().map(value -> new CsvProduct(ownerId, value)).toList()
            );
            return result.size();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "상품 CSV 처리 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 매출 CSV 파일을 읽어 일별 매출 데이터를 메모리에 저장한다.
     */
    @Transactional
    public int loadSales(Long ownerId, MultipartFile file) {
        List<DailySalesDto> result = new ArrayList<>();

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            validateHeaders(
                    parser,
                    "sale_date",
                    "product_code",
                    "product_name",
                    "sales_quantity",
                    "unit_price",
                    "sales_amount"
            );

            for (CSVRecord record : parser) {
                long quantity = Long.parseLong(
                        record.get("sales_quantity")
                );

                long unitPrice = Long.parseLong(
                        record.get("unit_price")
                );

                /*
                 * CSV 파일의 sales_amount 값을 그대로 사용하지 않고,
                 * 판매 수량과 단가를 이용해 서버에서 다시 계산한다.
                 */
                long salesAmount = quantity * unitPrice;

                DailySalesDto sale = new DailySalesDto(
                        LocalDate.parse(
                                record.get("sale_date")
                        ),
                        record.get("product_code"),
                        record.get("product_name"),
                        quantity,
                        unitPrice,
                        salesAmount
                );

                result.add(sale);
            }

            salesRepository.deleteAllByOwnerId(ownerId);
            salesRepository.flush();
            salesRepository.saveAll(
                    result.stream().map(value -> new CsvDailySales(ownerId, value)).toList()
            );
            return result.size();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "매출 CSV 처리 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 재고 CSV 파일을 읽어 원재료 재고 데이터를 메모리에 저장한다.
     */
    @Transactional
    public int loadInventories(Long ownerId, MultipartFile file) {
        List<InventoryDataRequest> result =
                new ArrayList<>();

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            validateHeaders(
                    parser,
                    "name",
                    "lot",
                    "stock",
                    "unit",
                    "expected_depletion",
                    "expiry",
                    "supplier",
                    "status",
                    "reorder_qty",
                    "supplier_price",
                    "lead_time"
            );

            for (CSVRecord record : parser) {
                double stock = Double.parseDouble(record.get("stock"));
                long reorderQty = Long.parseLong(record.get("reorder_qty"));
                long supplierPrice = Long.parseLong(record.get("supplier_price"));
                int leadTime = Integer.parseInt(record.get("lead_time"));

                InventoryDataRequest inventory =
                        new InventoryDataRequest(
                                record.get("name"),
                                record.get("lot"),
                                stock,
                                record.get("unit"),
                                record.get("expected_depletion"),
                                record.get("expiry"),
                                record.get("supplier"),
                                record.get("status"),
                                reorderQty,
                                supplierPrice,
                                leadTime
                        );

                result.add(inventory);
            }

            inventoryRepository.deleteAllByOwnerId(ownerId);
            inventoryRepository.flush();
            inventoryRepository.saveAll(
                    result.stream().map(value -> new CsvInventory(ownerId, value)).toList()
            );
            return result.size();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "재고 CSV 처리 실패: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * MultipartFile을 UTF-8 BufferedReader로 변환한다.
     *
     * BOM이 포함된 UTF-8 CSV도 읽을 수 있도록
     * BOMInputStream을 사용한다.
     */
    private BufferedReader createReader(MultipartFile file)
            throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV 파일이 비어 있습니다."
            );
        }

        BOMInputStream bomInputStream =
                BOMInputStream.builder()
                        .setInputStream(
                                file.getInputStream()
                        )
                        .get();

        return new BufferedReader(
                new InputStreamReader(
                        bomInputStream,
                        StandardCharsets.UTF_8
                )
        );
    }

    /**
     * CSV 첫 번째 줄을 헤더로 인식하는 CSVParser를 생성한다.
     */
    private CSVParser createParser(BufferedReader reader)
            throws Exception {

        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get()
                .parse(reader);
    }

    /**
     * CSV 파일에 필수 헤더가 존재하는지 검사한다.
     */
    private void validateHeaders(
            CSVParser parser,
            String... requiredHeaders
    ) {
        for (String header : requiredHeaders) {
            if (!parser.getHeaderMap().containsKey(header)) {
                throw new IllegalArgumentException(
                        "필수 헤더가 없습니다: " + header
                );
            }
        }
    }

    /**
     * 선택 헤더의 문자열 값을 읽는다.
     *
     * 헤더가 없거나 값이 비어 있으면 null을 반환한다.
     */
    private String getNullable(
            CSVRecord record,
            String header
    ) {
        if (!record.isMapped(header)) {
            return null;
        }

        String value = record.get(header);

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * 선택 헤더의 숫자 값을 읽는다.
     *
     * 헤더가 없거나 값이 비어 있으면 기본값을 반환한다.
     */
    private long getLongOrDefault(
            CSVRecord record,
            String header,
            long defaultValue
    ) {
        if (!record.isMapped(header)) {
            return defaultValue;
        }

        String value = record.get(header);

        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return Long.parseLong(value.trim());
    }

    /**
     * 현재 메모리에 저장된 상품 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<ProductDataRequest> getProducts(Long ownerId) {
        return productRepository.findAllByOwnerIdOrderByProductCode(ownerId)
                .stream().map(CsvProduct::toDto).toList();
    }

    /**
     * 현재 메모리에 저장된 매출 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<DailySalesDto> getSales(Long ownerId) {
        return salesRepository.findAllByOwnerIdOrderBySaleDateAscProductCodeAsc(ownerId)
                .stream().map(CsvDailySales::toDto).toList();
    }

    /**
     * 현재 메모리에 저장된 재고 목록을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<InventoryDataRequest> getInventories(Long ownerId) {
        return inventoryRepository.findAllByOwnerIdOrderByName(ownerId)
                .stream().map(CsvInventory::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatus(Long ownerId) {
        return Map.of(
                "productCount", productRepository.countByOwnerId(ownerId),
                "salesCount", salesRepository.countByOwnerId(ownerId),
                "inventoryCount", inventoryRepository.countByOwnerId(ownerId)
        );
    }
}
