package com.bp20.backend.api.order.service;

import com.bp20.backend.api.order.domain.Order;
import com.bp20.backend.api.order.domain.OrderItem;
import com.bp20.backend.api.order.repository.OrderItemRepository;
import com.bp20.backend.api.order.repository.OrderRepository;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 매출(Order/OrderItem)·메뉴(Product) CSV를 로그인한 점주 기준으로 업로드한다.
 *
 * 기존 api/csv 패키지의 CsvDataService는 발주 추천(AI 재고 예측) 학습용 CSV를 다루고,
 * 이 서비스가 저장하는 데이터는 그것과 완전히 별개로 AI 가계부 리포트(ReceiptAnalyticsService)가
 * 실제로 조회하는 "정식" 매출 데이터다. CSV 파싱 방식(Apache Commons CSV + BOM 처리)만 동일하게 맞췄다.
 *
 * PR #35 리뷰 코멘트 반영: 메뉴는 더 이상 별도 MenuItem 엔티티가 아니라 Product를 그대로 쓴다.
 * OrderItem이 Product를 FK로 참조하므로, 재업로드 시 기존 MenuItem처럼 전체 삭제 후 재삽입하면
 * 안 되고(참조 중인 OrderItem이 끊어짐) (store, sourceProductId) 기준으로 upsert한다.
 */
@Service
@RequiredArgsConstructor
public class OrderCsvImportService {

    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    /**
     * cafe_products.csv (ProductID,StoreID,ProductName,Category,Price,DiscountRate) 업로드.
     * CSV의 StoreID 컬럼은 신뢰하지 않고, 항상 로그인한 점주의 매장으로 귀속시킨다.
     * DiscountRate 컬럼은 더 이상 Product에 저장하지 않는다 - 할인이 필요하면 Discount
     * 테이블에 별도로 등록한다(PR #35 리뷰 코멘트).
     */
    @Transactional
    public int loadMenuItems(Long ownerId, MultipartFile file) {
        Store store = requireOwnedStore(ownerId);
        Map<Long, Product> existingBySourceId = productRepository.findByStoreIdOrderByIdDesc(store.getId())
                .stream()
                .filter(product -> product.getSourceProductId() != null)
                .collect(Collectors.toMap(Product::getSourceProductId, product -> product, (a, b) -> a));

        List<Product> created = new ArrayList<>();
        int updatedCount = 0;

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            validateHeaders(parser, "ProductID", "ProductName", "Price");

            for (CSVRecord record : parser) {
                Long sourceProductId = parseLong(record, "ProductID");
                String name = record.get("ProductName").trim();
                String category = getNullable(record, "Category");
                long price = parseLong(record, "Price");

                Product existing = existingBySourceId.get(sourceProductId);
                if (existing != null) {
                    existing.updateFromCsv(name, category, price);
                    updatedCount++;
                } else {
                    created.add(Product.createFromCsv(store, sourceProductId, name, category, price));
                }
            }

            productRepository.saveAll(created);
            return created.size() + updatedCount;

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        }
    }

    /**
     * cafe_sales_orders.csv (OrderID,StoreID,OrderType,TotalAmount,DiscountAmount,PaymentMethod,OrderedDate,OrderedTime) 업로드.
     * 주문을 다시 올리면 이전 주문에 딸려있던 주문상세(OrderItem)는 참조가 끊어지므로 함께 지운다.
     */
    @Transactional
    public int loadOrders(Long ownerId, MultipartFile file) {
        Store store = requireOwnedStore(ownerId);
        List<Order> result = new ArrayList<>();

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            validateHeaders(parser, "OrderID", "OrderType", "TotalAmount", "OrderedDate");

            for (CSVRecord record : parser) {
                String timeText = getNullable(record, "OrderedTime");
                result.add(Order.create(
                        store,
                        parseLong(record, "OrderID"),
                        record.get("OrderType").trim(),
                        parseLong(record, "TotalAmount"),
                        getLongOrDefault(record, "DiscountAmount", 0),
                        getNullable(record, "PaymentMethod"),
                        LocalDate.parse(record.get("OrderedDate").trim()),
                        timeText != null ? LocalTime.parse(timeText) : null
                ));
            }

            orderItemRepository.deleteAllByOrder_Store_Id(store.getId());
            orderRepository.deleteAllByStoreId(store.getId());
            orderRepository.flush();
            orderRepository.saveAll(result);
            return result.size();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        }
    }

    /**
     * cafe_sales_order_items.csv (OrderItemID,OrderID,ProductID,ProductName,Quantity,UnitPrice,TotalPrice) 업로드.
     * 반드시 loadOrders()와 loadMenuItems()를 먼저 올린 뒤 호출해야 한다 (OrderID로 이미 저장된
     * 주문을, ProductID로 이미 저장된 상품을 찾아 연결한다 - 이제 Product FK가 필수라 메뉴 CSV가
     * order-items보다 먼저 있어야 한다).
     */
    @Transactional
    public int loadOrderItems(Long ownerId, MultipartFile file) {
        Store store = requireOwnedStore(ownerId);
        List<OrderItem> result = new ArrayList<>();

        // CSV 한 줄마다 DB에서 주문/상품을 찾으면 수만 건 규모에서 매우 느려진다(N+1 쿼리).
        // 매장의 주문/상품을 한 번에 전부 불러와 메모리에서 source id로 찾도록 한다.
        Map<Long, Order> ordersBySourceId = orderRepository.findByStoreIdOrderByOrderedDateDesc(store.getId())
                .stream()
                .collect(Collectors.toMap(Order::getSourceOrderId, order -> order, (a, b) -> a));
        Map<Long, Product> productsBySourceId = productRepository.findByStoreIdOrderByIdDesc(store.getId())
                .stream()
                .filter(product -> product.getSourceProductId() != null)
                .collect(Collectors.toMap(Product::getSourceProductId, product -> product, (a, b) -> a));

        try (
                BufferedReader reader = createReader(file);
                CSVParser parser = createParser(reader)
        ) {
            validateHeaders(parser, "OrderID", "ProductID", "ProductName", "Quantity", "UnitPrice", "TotalPrice");

            for (CSVRecord record : parser) {
                long sourceOrderId = parseLong(record, "OrderID");
                Order order = ordersBySourceId.get(sourceOrderId);
                if (order == null) {
                    throw new ApiException(
                            ErrorCode.BAD_REQUEST_INVALID_INPUT,
                            new IllegalArgumentException(
                                    "주문상세 CSV의 OrderID(" + sourceOrderId + ")에 해당하는 주문이 없습니다. "
                                            + "주문(cafe_sales_orders.csv)을 먼저 업로드해주세요."
                            )
                    );
                }

                long sourceProductId = parseLong(record, "ProductID");
                Product product = productsBySourceId.get(sourceProductId);
                if (product == null) {
                    throw new ApiException(
                            ErrorCode.BAD_REQUEST_INVALID_INPUT,
                            new IllegalArgumentException(
                                    "주문상세 CSV의 ProductID(" + sourceProductId + ")에 해당하는 상품이 없습니다. "
                                            + "메뉴(cafe_products.csv)를 먼저 업로드해주세요."
                            )
                    );
                }

                result.add(OrderItem.create(
                        order,
                        product,
                        (int) parseLong(record, "Quantity"),
                        parseLong(record, "UnitPrice"),
                        parseLong(record, "TotalPrice")
                ));
            }

            orderItemRepository.deleteAllByOrder_Store_Id(store.getId());
            orderItemRepository.flush();
            orderItemRepository.saveAll(result);
            return result.size();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_INPUT, e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStatus(Long ownerId) {
        Store store = requireOwnedStore(ownerId);
        return Map.of(
                "menuItemCount", productRepository.countByStoreIdAndSourceProductIdIsNotNull(store.getId()),
                "orderCount", orderRepository.countByStoreId(store.getId()),
                "orderItemCount", orderItemRepository.countByOrder_Store_Id(store.getId())
        );
    }

    private Store requireOwnedStore(Long ownerId) {
        return storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
    }

    private long parseLong(CSVRecord record, String header) {
        return Long.parseLong(record.get(header).trim());
    }

    /**
     * MultipartFile을 UTF-8 BufferedReader로 변환한다 (BOM 포함 UTF-8도 처리).
     */
    private BufferedReader createReader(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new ApiException(
                    ErrorCode.BAD_REQUEST_INVALID_INPUT,
                    new IllegalArgumentException("CSV 파일이 비어 있습니다.")
            );
        }

        BOMInputStream bomInputStream = BOMInputStream.builder()
                .setInputStream(file.getInputStream())
                .get();

        return new BufferedReader(new InputStreamReader(bomInputStream, StandardCharsets.UTF_8));
    }

    private CSVParser createParser(BufferedReader reader) throws Exception {
        return CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get()
                .parse(reader);
    }

    private void validateHeaders(CSVParser parser, String... requiredHeaders) {
        for (String header : requiredHeaders) {
            if (!parser.getHeaderMap().containsKey(header)) {
                throw new ApiException(
                        ErrorCode.BAD_REQUEST_INVALID_INPUT,
                        new IllegalArgumentException("필수 헤더가 없습니다: " + header)
                );
            }
        }
    }

    private String getNullable(CSVRecord record, String header) {
        if (!record.isMapped(header)) {
            return null;
        }
        String value = record.get(header);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private long getLongOrDefault(CSVRecord record, String header, long defaultValue) {
        if (!record.isMapped(header)) {
            return defaultValue;
        }
        String value = record.get(header);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }
}
