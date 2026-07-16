package com.bp20.backend.inventory;

public record IngredientInventoryData(
        String ingredientName,
        long currentStock,
        long reservedStock,
        long incomingStock,
        long safetyStock,
        long orderUnit
) {

    public long availableStock() {
        return Math.max(0, currentStock - reservedStock);
    }
}