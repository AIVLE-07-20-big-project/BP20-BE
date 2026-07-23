package com.bp20.backend.product;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record ProductDataRequest(
        String productCode,
        String productName,
        String ingredient1,
        String ingredient2,
        String ingredient3,
        String ingredient4,
        String ingredient5
) {

    /**
     * ingredient1~5 중 실제 값이 존재하는 재료만 반환한다.
     *
     * 현재 가정:
     * 상품 1개가 판매될 때 각 재료가 1씩 소모된다.
     */
    public List<String> getIngredients() {
        return Stream.of(
                        ingredient1,
                        ingredient2,
                        ingredient3,
                        ingredient4,
                        ingredient5
                )
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }
}