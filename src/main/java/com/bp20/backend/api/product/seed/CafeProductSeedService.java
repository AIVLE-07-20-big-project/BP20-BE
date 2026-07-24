package com.bp20.backend.api.product.seed;

import com.bp20.backend.api.store.event.StoreCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@Profile({"local", "docker"})
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.seed.cafe-products.enabled",
        havingValue = "true"
)
public class CafeProductSeedService {

    private final DataSource dataSource;

    @Value("classpath:db/seed/cafe-products.sql")
    private Resource cafeProductsSql;

    public void seedExistingStores() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(cafeProductsSql);
        populator.setContinueOnError(false);
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
        log.info("개발용 카페 상품 더미데이터 시딩을 완료했습니다.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void seedNewStore(StoreCreatedEvent event) {
        seedExistingStores();
        log.info("신규 매장 카페 상품 더미데이터 시딩 완료: storeId={}", event.storeId());
    }
}
