package com.bp20.backend.csv;

import com.bp20.backend.inventory.IngredientInventoryData;
import com.bp20.backend.product.ProductData;
import com.bp20.backend.sales.DailySaleData;
import lombok.Getter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Getter
public class CsvDataService {

    /*
     * DB 대신 서버 메모리에 데이터 저장
     *
     * 서버를 종료하면 데이터가 사라진다.
     */
    private List<ProductData> products = new ArrayList<>();
    private List<DailySaleData> sales = new ArrayList<>();
    private List<IngredientInventoryData> inventories =
            new ArrayList<>();

    public void loadProducts(MultipartFile file) {
        List<ProductData> result = new ArrayList<>();

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
                result.add(
                        new ProductData(
                                record.get("product_code"),
                                record.get("product_name"),
                                getNullable(record, "ingredient1"),
                                getNullable(record, "ingredient2"),
                                getNullable(record, "ingredient3"),
                                getNullable(record, "ingredient4"),
                                getNullable(record, "ingredient5")
                        )
                );
            }

            /*
             * 파일 전체 파싱에 성공한 뒤 기존 메모리 데이터를 교체한다.
             * 중간에 오류가 나면 기존 데이터는 유지된다.
             */
            this.products = result;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "상품 CSV 처리 실패: " + e.getMessage(),
                    e
            );
        }
    }

    public void loadSales(MultipartFile file) {
        List<DailySaleData> result = new ArrayList<>();

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
                 * sales_amount는 CSV 값을 그대로 신뢰하지 않고
                 * 서버에서 다시 계산한다.
                 */
                long salesAmount = quantity * unitPrice;

                result.add(
                        new DailySaleData(
                                LocalDate.parse(record.get("sale_date")),
                                record.get("product_code"),
                                record.get("product_name"),
                                quantity,
                                unitPrice,
                                salesAmount
                        )
                );
            }

            this.sales = result;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "매출 CSV 처리 실패: " + e.getMessage(),
                    e
            );
        }
    }

    public void loadInventories(MultipartFile file) {
        List<IngredientInventoryData> result =
                new ArrayList<>();

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            /*
             * 현재 CSV의 실제 헤더명에 맞게 수정해야 한다.
             */
            validateHeaders(
                    parser,
                    "ingredient_name",
                    "current_stock"
            );

            for (CSVRecord record : parser) {
                /*
                 * CSV에 해당 컬럼이 없으면 기본값을 사용한다.
                 */
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

                result.add(
                        new IngredientInventoryData(
                                record.get("ingredient_name"),
                                Long.parseLong(
                                        record.get("current_stock")
                                ),
                                reservedStock,
                                incomingStock,
                                safetyStock,
                                orderUnit
                        )
                );
            }

            this.inventories = result;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "재고 CSV 처리 실패: " + e.getMessage(),
                    e
            );
        }
    }

    private BufferedReader createReader(MultipartFile file)
            throws Exception {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "CSV 파일이 비어 있습니다."
            );
        }

        BOMInputStream bomInputStream =
                BOMInputStream.builder()
                        .setInputStream(file.getInputStream())
                        .get();

        return new BufferedReader(
                new InputStreamReader(
                        bomInputStream,
                        StandardCharsets.UTF_8
                )
        );
    }

    private CSVParser createParser(BufferedReader reader)
            throws Exception {

        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get()
                .parse(reader);
    }

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

    private String getNullable(
            CSVRecord record,
            String header
    ) {
        if (!record.isMapped(header)) {
            return null;
        }

        String value = record.get(header);

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

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

        return Long.parseLong(value);
    }
}