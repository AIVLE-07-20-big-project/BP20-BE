package com.bp20.backend.csv;

import com.bp20.backend.inventory.InventoryDataRequest;
import com.bp20.backend.product.ProductDataRequest;
import com.bp20.backend.sales.DailySalesDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvDataService {

    /*
     * DB 대신 서버 메모리에 데이터를 저장한다.
     *
     * 서버를 종료하면 데이터가 모두 사라진다.
     */
    private List<ProductDataRequest> products = new ArrayList<>();
    private List<DailySalesDto> sales = new ArrayList<>();
    private List<InventoryDataRequest> inventories =
            new ArrayList<>();

    /**
     * 상품 CSV 파일을 읽어 상품 데이터를 메모리에 저장한다.
     */
    public void loadProducts(MultipartFile file) {
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
            this.products = result;

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
    public void loadSales(MultipartFile file) {
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

            this.sales = result;

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
    public void loadInventories(MultipartFile file) {
        List<InventoryDataRequest> result =
                new ArrayList<>();

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            /*
             * 반드시 존재해야 하는 최소 헤더만 검사한다.
             *
             * reserved_stock, incoming_stock, safety_stock,
             * order_unit은 없을 경우 기본값을 사용한다.
             */
            validateHeaders(
                    parser,
                    "ingredient_name",
                    "current_stock"
            );

            for (CSVRecord record : parser) {
                long currentStock = Long.parseLong(
                        record.get("current_stock")
                );

                long reservedStock = getLongOrDefault(
                        record,
                        "reserved_stock",
                        0
                );

                long incomingStock = getLongOrDefault(
                        record,
                        "incoming_stock",
                        0
                );

                long safetyStock = getLongOrDefault(
                        record,
                        "safety_stock",
                        10
                );

                long orderUnit = getLongOrDefault(
                        record,
                        "order_unit",
                        1
                );

                InventoryDataRequest inventory =
                        new InventoryDataRequest(
                                record.get("ingredient_name"),
                                currentStock,
                                reservedStock,
                                incomingStock,
                                safetyStock,
                                orderUnit
                        );

                result.add(inventory);
            }

            this.inventories = result;

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
    public List<ProductDataRequest> getProducts() {
        return products;
    }

    /**
     * 현재 메모리에 저장된 매출 목록을 반환한다.
     */
    public List<DailySalesDto> getSales() {
        return sales;
    }

    /**
     * 현재 메모리에 저장된 재고 목록을 반환한다.
     */
    public List<InventoryDataRequest> getInventories() {
        return inventories;
    }
}