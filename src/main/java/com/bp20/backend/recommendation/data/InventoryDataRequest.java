package com.bp20.backend.recommendation.data;

public record InventoryDataRequest(
        String name,
        String lot,
        double stock,
        String unit,
        String expectedDepletion,
        String expiry,
        String supplier,
        String status,
        long reorderQty,
        long supplierPrice,
        int leadTime
) {

    public String ingredientName() {
        return name;
    }

    public long currentStock() {
        return Math.max(0, Math.round(stock));
    }

    public long reservedStock() {
        return 0;
    }

    public long incomingStock() {
        return 0;
    }

    public long safetyStock() {
        return 10;
    }

    public long orderUnit() {
        return 1;
    }

    public long availableStock() {
        return Math.max(0, currentStock() - reservedStock());
    }
}
