package com.bp20.backend.api.product.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.seed.cafe-products.enabled",
        havingValue = "true"
)
public class CafeProductSeedRunner implements ApplicationRunner {

    private final CafeProductSeedService cafeProductSeedService;

    @Override
    public void run(ApplicationArguments args) {
        cafeProductSeedService.seedExistingStores();
    }
}
