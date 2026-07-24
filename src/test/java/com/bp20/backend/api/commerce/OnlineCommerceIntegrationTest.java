package com.bp20.backend.api.commerce;

import com.bp20.backend.api.commerce.domain.CouponStatus;
import com.bp20.backend.api.commerce.domain.DiscountStatus;
import com.bp20.backend.api.commerce.domain.DiscountType;
import com.bp20.backend.api.commerce.dto.request.CreateDiscountRequest;
import com.bp20.backend.api.commerce.dto.request.DiscountStatusRequest;
import com.bp20.backend.api.commerce.dto.request.IssueCouponRequest;
import com.bp20.backend.api.commerce.dto.request.OnlineSalesStatusRequest;
import com.bp20.backend.api.commerce.dto.response.CouponResponse;
import com.bp20.backend.api.commerce.dto.response.DiscountResponse;
import com.bp20.backend.api.commerce.service.CouponService;
import com.bp20.backend.api.commerce.service.DiscountService;
import com.bp20.backend.api.commerce.service.OnlineSalesService;
import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.customer.dto.request.CreateCustomerRequest;
import com.bp20.backend.api.customer.dto.response.CustomerResponse;
import com.bp20.backend.api.customer.repository.CustomerRepository;
import com.bp20.backend.api.customer.service.CustomerService;
import com.bp20.backend.api.product.domain.OnlineSalesStatus;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.dto.request.CreateProductRequest;
import com.bp20.backend.api.product.dto.request.UpdateProductRequest;
import com.bp20.backend.api.product.dto.response.ProductResponse;
import com.bp20.backend.api.product.service.ProductService;
import com.bp20.backend.api.store.dto.request.CreateStoreRequest;
import com.bp20.backend.api.store.dto.response.StoreResponse;
import com.bp20.backend.api.store.service.StoreService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private DiscountService discountService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void storeOwnerCanOpenOnlineSalesWithRegisteredProduct() {
        User owner = createOwner("commerce-owner@example.com");
        createStore(owner, "123-45-67890");
        ProductResponse coffee = createProduct(owner, "아메리카노", 4_000);
        registerOnline(owner, coffee.id());

        StoreResponse opened = onlineSalesService.changeStatus(
                owner.getId(),
                new OnlineSalesStatusRequest(com.bp20.backend.api.store.domain.OnlineSalesStatus.OPEN)
        );

        assertThat(opened.onlineSalesStatus()).isEqualTo(com.bp20.backend.api.store.domain.OnlineSalesStatus.OPEN);
    }

    @Test
    void commonDiscountCanTargetProductWithoutOnlineRegistration() {
        User owner = createOwner("discount-owner@example.com");
        createStore(owner, "121-22-23232");
        ProductResponse product = createProduct(owner, "오프라인 판매 상품", 5_000);
        LocalDateTime now = LocalDateTime.now();

        DiscountResponse discount = discountService.create(
                owner.getId(),
                new CreateDiscountRequest(
                        "오후 상품 할인",
                        "온·오프라인 공통 할인",
                        DiscountType.RATE,
                        10,
                        product.id(),
                        now.minusMinutes(1),
                        now.plusDays(1),
                        LocalTime.of(14, 0),
                        LocalTime.of(17, 0),
                        false
                )
        );
        DiscountResponse activated = discountService.changeStatus(
                owner.getId(),
                discount.id(),
                new DiscountStatusRequest(DiscountStatus.ACTIVE)
        );

        assertThat(activated.status()).isEqualTo(DiscountStatus.ACTIVE);
        assertThat(activated.product().id()).isEqualTo(product.id());
    }

    @Test
    void productMustBeRegisteredSeparatelyForOnlineSales() {
        User owner = createOwner("product-channel-owner@example.com");
        createStore(owner, "777-88-99999");
        ProductResponse product = createProduct(owner, "시그니처 상품", 5_000);

        ProductResponse registered = onlineSalesService.registerProduct(owner.getId(), product.id());
        ProductResponse soldOut = productService.update(
                owner.getId(),
                product.id(),
                new UpdateProductRequest(
                        product.name(),
                        product.description(),
                        product.price(),
                        0,
                        product.imageUrl()
                )
        );
        ProductResponse restocked = productService.update(
                owner.getId(),
                product.id(),
                new UpdateProductRequest(
                        product.name(),
                        product.description(),
                        product.price(),
                        10,
                        product.imageUrl()
                )
        );
        ProductResponse registeredAgain = onlineSalesService.registerProduct(owner.getId(), product.id());
        ProductResponse unregistered = onlineSalesService.unregisterProduct(owner.getId(), product.id());

        assertThat(product.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.NOT_REGISTERED);
        assertThat(registered.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.ON_SALE);
        assertThat(soldOut.status()).isEqualTo(ProductStatus.SOLD_OUT);
        assertThat(soldOut.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.NOT_REGISTERED);
        assertThat(restocked.status()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(restocked.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.NOT_REGISTERED);
        assertThat(registeredAgain.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.ON_SALE);
        assertThat(unregistered.onlineSalesStatus()).isEqualTo(OnlineSalesStatus.NOT_REGISTERED);
    }

    @Test
    void storeOwnerCanIssueCouponToCustomerWhosePrivateInfoIsSeparated() {
        User owner = createOwner("coupon-owner@example.com");
        createStore(owner, "555-66-77777");
        CustomerResponse customer = customerService.create(
                owner.getId(),
                new CreateCustomerRequest(
                        "customer@example.com",
                        "김고객",
                        "010-3333-3333"
                )
        );

        CouponResponse coupon = couponService.issue(
                owner.getId(),
                new IssueCouponRequest(
                        customer.id(),
                        "신규 고객 3,000원 쿠폰",
                        DiscountType.FIXED_AMOUNT,
                        3_000,
                        LocalDateTime.now().plusDays(7)
                )
        );

        Customer savedCustomer = customerRepository.findById(customer.id()).orElseThrow();
        assertThat(savedCustomer.getPrivateInfo().getId()).isNotNull();
        assertThat(savedCustomer.getPrivateInfo().getPasswordHash()).isNull();
        assertThat(coupon.status()).isEqualTo(CouponStatus.ISSUED);
        assertThat(coupon.customerId()).isEqualTo(customer.id());
        assertThat(coupon.customerEmail()).isEqualTo("customer@example.com");
        assertThat(coupon.discountValue()).isEqualTo(3_000);
        assertThat(customerService.getMine(owner.getId())).singleElement()
                .extracting(CustomerResponse::id)
                .isEqualTo(customer.id());
    }

    @Test
    void customerEmailCannotBeDuplicatedWithinSameStore() {
        User owner = createOwner("customer-owner@example.com");
        createStore(owner, "556-67-78888");
        customerService.create(
                owner.getId(),
                new CreateCustomerRequest("customer@example.com", "첫 고객", "010-1111-1111")
        );

        assertThatThrownBy(() -> customerService.create(
                owner.getId(),
                new CreateCustomerRequest("CUSTOMER@example.com", "중복 고객", "010-2222-2222")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT_DUPLICATE_CUSTOMER_EMAIL);
    }

    @Test
    void storeOwnerCannotIssueCouponToAnotherStoresCustomer() {
        User firstOwner = createOwner("first-customer-owner@example.com");
        createStore(firstOwner, "557-68-79999");
        CustomerResponse customer = customerService.create(
                firstOwner.getId(),
                new CreateCustomerRequest("first-customer@example.com", "첫 매장 고객", null)
        );

        User secondOwner = createOwner("second-customer-owner@example.com");
        createStore(secondOwner, "558-69-80000");

        assertThatThrownBy(() -> couponService.issue(
                secondOwner.getId(),
                new IssueCouponRequest(
                        customer.id(),
                        "다른 매장 쿠폰",
                        DiscountType.RATE,
                        10,
                        LocalDateTime.now().plusDays(7)
                )
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND_CUSTOMER);
    }

    @Test
    void onlineSalesCannotOpenWithoutAnOnSaleProduct() {
        User owner = createOwner("empty-store-owner@example.com");
        createStore(owner, "111-22-33333");

        assertThatThrownBy(() -> onlineSalesService.changeStatus(
                owner.getId(),
                new OnlineSalesStatusRequest(com.bp20.backend.api.store.domain.OnlineSalesStatus.OPEN)
        ))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST_INVALID_STORE_STATUS);
    }

    @Test
    void onlineSalesCannotOpenWithOnlySoldOutOnlineProduct() {
        User owner = createOwner("sold-out-store-owner@example.com");
        createStore(owner, "112-23-34444");
        ProductResponse product = createProduct(owner, "재고 없는 온라인 상품", 5_000);
        onlineSalesService.registerProduct(owner.getId(), product.id());
        productService.update(
                owner.getId(),
                product.id(),
                new UpdateProductRequest(
                        product.name(),
                        product.description(),
                        product.price(),
                        0,
                        product.imageUrl()
                )
        );

        assertThatThrownBy(() -> onlineSalesService.changeStatus(
                owner.getId(),
                new OnlineSalesStatusRequest(com.bp20.backend.api.store.domain.OnlineSalesStatus.OPEN)
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
        ProductResponse secondOwnersProduct = createProduct(secondOwner, "다른 매장 상품", 5_000);

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

    private StoreResponse createStore(User owner, String businessNumber) {
        return storeService.create(
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
}
