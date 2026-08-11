package com.bp20.backend.api.csv;

import com.bp20.backend.api.csv.service.CsvDataService;
import com.bp20.backend.api.product.domain.OnlineSalesStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.dto.request.CreateStoreRequest;
import com.bp20.backend.api.store.service.StoreService;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.api.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CsvProductUploadIntegrationTest {

    @Autowired
    private CsvDataService csvDataService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void productCsvCreatesMissingStoreProductsAndDoesNotDuplicateThemOnReupload() {
        User owner = createOwner();
        createStore(owner);
        MockMultipartFile file = productCsv();

        int firstUploadCount = csvDataService.loadProducts(owner.getId(), file);
        int secondUploadCount = csvDataService.loadProducts(owner.getId(), productCsv());

        List<Product> products = productRepository.findByStore_Owner_IdOrderByIdAsc(owner.getId());
        assertThat(firstUploadCount).isEqualTo(2);
        assertThat(secondUploadCount).isEqualTo(2);
        assertThat(products).hasSize(2);

        Product americano = products.stream()
                .filter(product -> product.getName().equals("아메리카노_HOT"))
                .findFirst()
                .orElseThrow();
        assertThat(americano.getPrice()).isEqualTo(3_500);
        assertThat(americano.getStockQuantity()).isNull();
        assertThat(americano.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(americano.getOnlineSalesStatus()).isEqualTo(OnlineSalesStatus.NOT_REGISTERED);

        Product beans = products.stream()
                .filter(product -> product.getName().equals("시그니처 원두 500g"))
                .findFirst()
                .orElseThrow();
        assertThat(beans.getPrice()).isEqualTo(18_000);
        assertThat(beans.getStockQuantity()).isEqualTo(0);
        assertThat(beans.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    private MockMultipartFile productCsv() {
        String csv = """
                product_code,product_name,price,stock_quantity,description,image_url,ingredient1,ingredient2,ingredient3,ingredient4,ingredient5
                P001,아메리카노_HOT,3500,,고소한 아메리카노,,원두,,,,
                P002,시그니처 원두 500g,18000,0,시그니처 원두,,원두,,,,
                """;
        return new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }

    private User createOwner() {
        return userRepository.save(User.createStoreOwner(
                "csv-product-owner@example.com",
                "CSV 점주",
                "010-1234-5678",
                passwordEncoder.encode("Passw0rd!234")
        ));
    }

    private void createStore(User owner) {
        storeService.create(
                owner.getId(),
                new CreateStoreRequest(
                        "CSV 테스트 매장",
                        "123-45-67890",
                        "카페",
                        "서울특별시 중구 세종대로 1",
                        "02-1234-5678"
                )
        );
    }
}
