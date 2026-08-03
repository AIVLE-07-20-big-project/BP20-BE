package com.bp20.backend.api.csv.domain;

import com.bp20.backend.api.recommendation.dto.ProductDataRequest;
import com.bp20.backend.api.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "csv_products", uniqueConstraints = @UniqueConstraint(
        name = "uk_csv_product_owner_code", columnNames = {"owner_id", "product_code"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CsvProduct {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;
    @Column(length = 100) private String ingredient1;
    @Column(length = 100) private String ingredient2;
    @Column(length = 100) private String ingredient3;
    @Column(length = 100) private String ingredient4;
    @Column(length = 100) private String ingredient5;

    public CsvProduct(User owner, ProductDataRequest value) {
        this.owner = owner;
        this.productCode = value.productCode();
        this.productName = value.productName();
        this.ingredient1 = value.ingredient1();
        this.ingredient2 = value.ingredient2();
        this.ingredient3 = value.ingredient3();
        this.ingredient4 = value.ingredient4();
        this.ingredient5 = value.ingredient5();
    }

    public ProductDataRequest toDto() {
        return new ProductDataRequest(productCode, productName, ingredient1, ingredient2, ingredient3, ingredient4, ingredient5);
    }
}
