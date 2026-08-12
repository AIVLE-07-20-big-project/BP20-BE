package com.bp20.backend.api.commerce.order.service;

import com.bp20.backend.api.commerce.order.domain.OnlinePurchase;
import com.bp20.backend.api.commerce.order.dto.request.CreateOnlinePurchaseRequest;
import com.bp20.backend.api.commerce.order.dto.response.OnlinePurchaseResponse;
import com.bp20.backend.api.commerce.order.repository.OnlinePurchaseRepository;
import com.bp20.backend.api.customer.domain.Customer;
import com.bp20.backend.api.customer.repository.CustomerRepository;
import com.bp20.backend.api.product.domain.OnlineSalesStatus;
import com.bp20.backend.api.product.domain.Product;
import com.bp20.backend.api.product.domain.ProductStatus;
import com.bp20.backend.api.product.repository.ProductRepository;
import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.store.repository.StoreRepository;
import com.bp20.backend.global.exception.ApiException;
import com.bp20.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OnlinePurchaseService {

    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OnlinePurchaseRepository onlinePurchaseRepository;

    @Transactional
    public OnlinePurchaseResponse record(Long ownerId, CreateOnlinePurchaseRequest request) {
        Store store = storeRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_STORE));
        if (store.getOnlineSalesStatus() != com.bp20.backend.api.store.domain.OnlineSalesStatus.OPEN) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PURCHASE);
        }
        Customer customer = customerRepository.findOwnedCustomer(request.customerId(), ownerId)
                .filter(Customer::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_CUSTOMER));

        Map<Long, Integer> quantities = mergeQuantities(request.items());
        Map<Product, Integer> products = new LinkedHashMap<>();
        long totalAmount = 0;
        try {
            for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
                Product product = productRepository.findOwnedProduct(entry.getKey(), ownerId)
                        .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_PRODUCT));
                validatePurchasable(product, entry.getValue());
                products.put(product, entry.getValue());
                totalAmount = Math.addExact(totalAmount, Math.multiplyExact(product.getPrice(), entry.getValue()));
            }
        } catch (ArithmeticException exception) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PURCHASE);
        }

        OnlinePurchase purchase = OnlinePurchase.create(
                store,
                customer,
                request.purchasedAt() == null ? LocalDateTime.now() : request.purchasedAt(),
                totalAmount
        );
        products.forEach((product, quantity) -> {
            purchase.addItem(product, quantity);
            product.decreaseStock(quantity);
        });
        return OnlinePurchaseResponse.from(onlinePurchaseRepository.save(purchase));
    }

    @Transactional(readOnly = true)
    public List<OnlinePurchaseResponse> getMine(Long ownerId) {
        return onlinePurchaseRepository.findAllOwnedBy(ownerId).stream()
                .map(OnlinePurchaseResponse::from)
                .toList();
    }

    private Map<Long, Integer> mergeQuantities(List<CreateOnlinePurchaseRequest.Item> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        try {
            for (CreateOnlinePurchaseRequest.Item item : items) {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            }
        } catch (ArithmeticException exception) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PURCHASE);
        }
        return quantities;
    }

    private void validatePurchasable(Product product, int quantity) {
        boolean unavailable = product.getStatus() != ProductStatus.ACTIVE
                || product.getOnlineSalesStatus() != OnlineSalesStatus.ON_SALE
                || (product.getStockQuantity() != null && product.getStockQuantity() < quantity);
        if (unavailable) {
            throw new ApiException(ErrorCode.BAD_REQUEST_INVALID_ONLINE_PURCHASE);
        }
    }
}
