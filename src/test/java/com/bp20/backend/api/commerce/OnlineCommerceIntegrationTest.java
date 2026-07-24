package com.bp20.backend.api.commerce;

import com.bp20.backend.api.commerce.domain.BundleStatus;
import com.bp20.backend.api.commerce.domain.CouponStatus;
import com.bp20.backend.api.commerce.domain.DiscountStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.dto.request.BundleItemRequest;
import com.bp20.backend.api.commerce.dto.request.BundleStatusRequest;
import com.bp20.backend.api.commerce.dto.request.CreateOnlineDiscountRequest;
import com.bp20.backend.api.commerce.dto.request.CreateProductBundleRequest;
import com.bp20.backend.api.commerce.dto.request.IssueCouponRequest;
import com.bp20.backend.api.commerce.dto.request.OnlineDiscountStatusRequest;
import com.bp20.backend.api.commerce.dto.request.OnlineSalesStatusRequest;
import com.bp20.backend.api.commerce.dto.request.UpdateProductBundleRequest;
import com.bp20.backend.api.commerce.dto.response.BundleItemResponse;
import com.bp20.backend.api.commerce.dto.response.CustomerCouponResponse;
import com.bp20.backend.api.commerce.dto.response.OnlineDiscountResponse;
import com.bp20.backend.api.commerce.dto.response.ProductBundleResponse;
import com.bp20.backend.api.commerce.service.CustomerCouponService;
import com.bp20.backend.api.commerce.service.OnlineDiscountService;
import com.bp20.backend.api.commerce.service.OnlineSalesService;
import com.bp20.backend.api.commerce.service.ProductBundleService;
import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.customer.repository.CustomerRepository;
import com.bp20.backend.api.product.domain.OnlineSalesItemStatus;
import com.bp20.backend.api.product.dto.request.CreateProductRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.product.service.ProductService;
import com.bp20.backend.api.store.dto.request.CreateStoreRequest;
import com.bp20.backend.api.store.domain.OnlineSalesStatus;
import com.bp20.backend.api.store.dto.response.StoreResponse;
import com.bp20.backend.api.store.service.StoreService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class OnlineCommerceIntegrationTest {

    @Autowired
    private StoreService storeService;

    @Autowired
    private OnlineSalesService onlineSalesService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OnlineDiscountService onlineDiscountService;

    @Autowired
    private ProductBundleService productBundleService;

    @Autowired
    private CustomerCouponService customerCouponService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Test
    void storeOwnerCanOpenOnlineSalesAndActivateAutomaticDiscount() {
        User owner = createOwner("commerce-owner@example.com");
        createStore(owner, "123-45-67890");

        ProductResponse coffee = createProduct(owner, "아메리카노", 4_000);
        ProductResponse sandwich = createProduct(owner, "샌드위치", 6_000);
        registerOnline(owner, coffee.id());
        registerOnline(owner, sandwich.id());
        ProductBundleResponse bundle = productBundleService.create(
                owner.getId(),
                new CreateProductBundleRequest(
                        "커피 샌드위치 세트",
                        "온라인 할인 대상 세트",
                        8_500,
                        null,
                        List.of(
                                new BundleItemRequest(coffee.id(), 1),
                                new BundleItemRequest(sandwich.id(), 1)
                        )
                )
        );
        productBundleService.changeStatus(
                owner.getId(),
                bundle.id(),
                new BundleStatusRequest(BundleStatus.ON_SALE)
        );
        onlineSalesService.registerBundle(owner.getId(), bundle.id());

        StoreResponse opened = onlineSalesService.changeStatus(
                owner.getId(),
                new OnlineSalesStatusRequest(OnlineSalesStatus.OPEN)
        );

        LocalDateTime now = LocalDateTime.now();
        OnlineDiscountResponse discount = onlineDiscountService.create(
                owner.getId(),
                new CreateOnlineDiscountRequest(
                        "오후 상품 할인",
                        "매출 저조 시간대에 대상 상품을 자동 할인합니다.",
                        DiscountType.RATE,
                        15,
                        List.of(coffee.id(), sandwich.id()),
                        List.of(bundle.id()),
                        now.minusMinutes(1),
                        now.plusDays(7),
                        LocalTime.of(14, 0),
                        LocalTime.of(17, 0),
                        true
                )
        );
        OnlineDiscountResponse activated = onlineDiscountService.changeStatus(
                owner.getId(),
                discount.id(),
                new OnlineDiscountStatusRequest(DiscountStatus.ACTIVE)
        );

        assertThat(opened.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.OPEN);
        assertThat(activated.status()).isEqualTo(DiscountStatus.ACTIVE);
        assertThat(activated.products()).hasSize(2);
        assertThat(activated.bundles()).hasSize(1);
        assertThat(activated.reminderEnabled()).isTrue();
    }

    @Test
    void onlineDiscountRejectsProductThatIsNotRegisteredForOnlineSales() {
        User owner = createOwner("discount-target-owner@example.com");
        createStore(owner, "121-22-23232");
        ProductResponse product = createProduct(owner, "오프라인 상품", 5_000);
        LocalDateTime now = LocalDateTime.now();

        assertThatThrownBy(() -> onlineDiscountService.create(
                owner.getId(),
                new CreateOnlineDiscountRequest(
                        "잘못된 온라인 할인",
                        "온라인 미등록 상품에는 적용할 수 없습니다.",
                        DiscountType.RATE,
                        10,
                        List.of(product.id()),
                        List.of(),
                        now.plusHours(1),
                        now.plusDays(1),
                        null,
                        null,
                        false
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST_INVALID_DISCOUNT);
    }

    @Test
    void storeProductMustBeRegisteredSeparatelyForOnlineSales() {
        User owner = createOwner("product-channel-owner@example.com");
        createStore(owner, "777-88-99999");

        ProductResponse product = createProduct(
                owner,
                "오프라인 우선 상품",
                5_000
        );
        ProductResponse registered = onlineSalesService.registerProduct(
                owner.getId(),
                product.id()
        );
        ProductResponse unregistered = onlineSalesService.unregisterProduct(
                owner.getId(),
                product.id()
        );

        assertThat(product.onlineSalesStatus()).isEqualTo(OnlineSalesItemStatus.NOT_REGISTERED);
        assertThat(registered.onlineSalesStatus()).isEqualTo(OnlineSalesItemStatus.ON_SALE);
        assertThat(unregistered.onlineSalesStatus()).isEqualTo(OnlineSalesItemStatus.NOT_REGISTERED);
    }

    @Test
    void cafeProductSeedSqlIsIdempotent() {
        User owner = createOwner("cafe-seed-owner@example.com");
        createStore(owner, "888-99-00000");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/seed/cafe-products.sql")
        );
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
        populator.execute(dataSource);

        List<ProductResponse> products = productService.getMine(owner.getId());

        assertThat(products).hasSize(16);
        assertThat(products)
                .extracting(ProductResponse::name)
                .contains("아이스 아메리카노", "클럽 샌드위치", "소금빵", "플레인 베이글");
        assertThat(products)
                .allMatch(product ->
                        product.onlineSalesStatus() == OnlineSalesItemStatus.NOT_REGISTERED
                );
        assertThat(productRepository.count()).isEqualTo(16);
    }

    @Test
    void storeOwnerCanCreateDiscountedProductBundle() {
        User owner = createOwner("bundle-owner@example.com");
        createStore(owner, "444-55-66666");

        ProductResponse coffee = createProduct(owner, "아메리카노", 4_000);
        ProductResponse sandwich = createProduct(owner, "샌드위치", 6_000);

        ProductBundleResponse bundle = productBundleService.create(
                owner.getId(),
                new CreateProductBundleRequest(
                        "브런치 커플 세트",
                        "아메리카노 2잔과 샌드위치 1개 세트",
                        12_000,
                        "https://example.com/brunch-set.jpg",
                        List.of(
                                new BundleItemRequest(coffee.id(), 2),
                                new BundleItemRequest(sandwich.id(), 1)
                        )
                )
        );
        ProductBundleResponse onSale = productBundleService.changeStatus(
                owner.getId(),
                bundle.id(),
                new BundleStatusRequest(BundleStatus.ON_SALE)
        );
        ProductBundleResponse registeredOnline = onlineSalesService.registerBundle(
                owner.getId(),
                onSale.id()
        );

        assertThat(onSale.status()).isEqualTo(BundleStatus.ON_SALE);
        assertThat(registeredOnline.onlineSalesStatus()).isEqualTo(OnlineSalesItemStatus.ON_SALE);
        assertThat(onSale.regularPrice()).isEqualTo(14_000);
        assertThat(onSale.bundlePrice()).isEqualTo(12_000);
        assertThat(onSale.discountAmount()).isEqualTo(2_000);
        assertThat(onSale.items()).hasSize(2);
    }

    @Test
    void storeOwnerCanUpdateBundleWhileKeepingExistingProducts() {
        User owner = createOwner("bundle-update-owner@example.com");
        createStore(owner, "999-00-11111");

        ProductResponse coffee = createProduct(owner, "아메리카노", 4_000);
        ProductResponse sandwich = createProduct(owner, "샌드위치", 6_000);
        ProductBundleResponse bundle = productBundleService.create(
                owner.getId(),
                new CreateProductBundleRequest(
                        "브런치 세트",
                        "아메리카노와 샌드위치 세트",
                        8_000,
                        null,
                        List.of(
                                new BundleItemRequest(coffee.id(), 1),
                                new BundleItemRequest(sandwich.id(), 1)
                        )
                )
        );
        entityManager.flush();

        ProductBundleResponse updated = productBundleService.update(
                owner.getId(),
                bundle.id(),
                new UpdateProductBundleRequest(
                        "브런치 커플 세트",
                        "아메리카노 2잔과 샌드위치 세트",
                        11_500,
                        "https://example.com/updated-brunch-set.jpg",
                        List.of(
                                new BundleItemRequest(coffee.id(), 2),
                                new BundleItemRequest(sandwich.id(), 1)
                        )
                )
        );
        entityManager.flush();

        assertThat(updated.name()).isEqualTo("브런치 커플 세트");
        assertThat(updated.regularPrice()).isEqualTo(14_000);
        assertThat(updated.bundlePrice()).isEqualTo(11_500);
        assertThat(updated.items())
                .filteredOn(item -> item.productId().equals(coffee.id()))
                .singleElement()
                .extracting(BundleItemResponse::quantity)
                .isEqualTo(2);
    }

    @Test
    void storeOwnerCanIssueCouponToSpecificCustomer() {
        User owner = createOwner("coupon-owner@example.com");
        createStore(owner, "555-66-77777");

        LocalDateTime now = LocalDateTime.now();
        Customer customer = customerRepository.save(Customer.create(
                "customer@example.com",
                "김고객",
                "010-3333-3333"
        ));

        CustomerCouponResponse coupon = customerCouponService.issue(
                owner.getId(),
                new IssueCouponRequest(
                        customer.getId(),
                        "신규 고객 3,000원 쿠폰",
                        DiscountType.FIXED_AMOUNT,
                        3_000,
                        now.plusDays(7)
                )
        );

        assertThat(coupon.status()).isEqualTo(CouponStatus.ISSUED);
        assertThat(coupon.name()).isEqualTo("신규 고객 3,000원 쿠폰");
        assertThat(coupon.customerId()).isEqualTo(customer.getId());
        assertThat(coupon.customerEmail()).isEqualTo("customer@example.com");
        assertThat(coupon.discountValue()).isEqualTo(3_000);
        assertThat(coupon.code()).isNotBlank();
    }

    @Test
    void onlineSalesCannotOpenWithoutAnOnSaleProduct() {
        User owner = createOwner("empty-store-owner@example.com");
        createStore(owner, "111-22-33333");

        assertThatThrownBy(() -> onlineSalesService.changeStatus(
                owner.getId(),
                new OnlineSalesStatusRequest(OnlineSalesStatus.OPEN)
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST_INVALID_STORE_STATUS);
    }

    @Test
    void storeOwnerCannotReadAnotherOwnersProduct() {
        User firstOwner = createOwner("first-owner@example.com");
        createStore(firstOwner, "222-33-44444");

        User secondOwner = createOwner("second-owner@example.com");
        createStore(secondOwner, "333-44-55555");
        ProductResponse secondOwnersProduct = createProduct(
                secondOwner,
                "다른 매장 상품",
                5_000
        );

        assertThatThrownBy(() -> productService.getOne(
                firstOwner.getId(),
                secondOwnersProduct.id()
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND_PRODUCT);
    }

    private User createOwner(String email) {
        return userRepository.save(User.createStoreOwner(
                email,
                "테스트 점주",
                "010-1234-5678",
                passwordEncoder.encode("Passw0rd!234")
        ));
    }

    private void createStore(User owner, String businessNumber) {
        storeService.create(
                owner.getId(),
                new CreateStoreRequest(
                        "테스트 매장",
                        businessNumber,
                        "카페",
                        "서울시 강남구 테헤란로 1",
                        "02-1234-5678"
                )
        );
    }

    private ProductResponse createProduct(User owner, String name, long price) {
        return productService.create(
                owner.getId(),
                new CreateProductRequest(
                        name,
                        name + " 상품 설명",
                        price,
                        10,
                        "https://example.com/product.jpg"
                )
        );
    }

    private void registerOnline(User owner, Long productId) {
        onlineSalesService.registerProduct(owner.getId(), productId);
    }

    private void openOnlineSales(User owner) {
        onlineSalesService.changeStatus(
                owner.getId(),
                new OnlineSalesStatusRequest(OnlineSalesStatus.OPEN)
        );
    }
}
