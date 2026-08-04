package com.bp20.backend.api.receipt.domain;

import com.bp20.backend.api.store.domain.Store;
import com.bp20.backend.api.user.domain.User;
import com.bp20.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 영수증(지출) 1건.
 *
 * ⚠️ storeId는 아직 Store 엔티티가 이 백엔드에 없어서 우선 순수 컬럼(Long)으로 둔다.
 *    나중에 Store 엔티티가 추가되면 @ManyToOne Store store 로 교체 예정.
 */
@Getter
@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipts_store_id", columnList = "store_id"),
        @Index(name = "idx_receipts_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_receipts_dedupe_key", columnList = "dedupe_key", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(nullable = false, length = 30)
    private String documentType;

    @Column(length = 100)
    private String vendorName;

    @Column(length = 20)
    private String businessNumber;

    @Column(nullable = false)
    private LocalDate transactionDate;

    private LocalTime transactionTime;

    @Column(nullable = false, length = 30)
    private String paymentMethod;

    @Column(nullable = false, length = 30)
    private String category;

    private Integer supplyAmount;

    private Integer vat;

    @Column(nullable = false)
    private Integer taxFreeAmount;

    @Column(nullable = false)
    private Integer totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceiptStatus status;

    @Column(nullable = false, unique = true, length = 150)
    private String dedupeKey;

    @Column(length = 255)
    private String rawImagePath;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private final List<ReceiptItem> items = new ArrayList<>();

    private Receipt(Store store, User uploadedBy, String documentType, String vendorName,
                     String businessNumber, LocalDate transactionDate, LocalTime transactionTime,
                     String paymentMethod, String category, Integer supplyAmount, Integer vat,
                     Integer taxFreeAmount, Integer totalAmount, String dedupeKey, String rawImagePath,
                     ReceiptStatus status) {
        this.store = store;
        this.uploadedBy = uploadedBy;
        this.documentType = documentType;
        this.vendorName = vendorName;
        this.businessNumber = businessNumber;
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.supplyAmount = supplyAmount;
        this.vat = vat;
        this.taxFreeAmount = taxFreeAmount == null ? 0 : taxFreeAmount;
        this.totalAmount = totalAmount;
        this.dedupeKey = dedupeKey;
        this.rawImagePath = rawImagePath;
        this.status = status;
    }

    public static Receipt create(Store store, User uploadedBy, String documentType, String vendorName,
                                  String businessNumber, LocalDate transactionDate, LocalTime transactionTime,
                                  String paymentMethod, String category, Integer supplyAmount, Integer vat,
                                  Integer taxFreeAmount, Integer totalAmount, String dedupeKey,
                                  String rawImagePath, ReceiptStatus status) {
        return new Receipt(store, uploadedBy, documentType, vendorName, businessNumber,
                transactionDate, transactionTime, paymentMethod, category, supplyAmount, vat,
                taxFreeAmount, totalAmount, dedupeKey, rawImagePath, status);
    }

    public void addItem(ReceiptItem item) {
        this.items.add(item);
        item.assignReceipt(this);
    }

    public void update(String documentType, String vendorName, String businessNumber,
                       LocalDate transactionDate, LocalTime transactionTime, String paymentMethod,
                       String category, Integer supplyAmount, Integer vat, Integer taxFreeAmount,
                       Integer totalAmount, String dedupeKey) {
        this.documentType = documentType;
        this.vendorName = vendorName;
        this.businessNumber = businessNumber;
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.supplyAmount = supplyAmount;
        this.vat = vat;
        this.taxFreeAmount = taxFreeAmount == null ? 0 : taxFreeAmount;
        this.totalAmount = totalAmount;
        this.dedupeKey = dedupeKey;
    }

    public void replaceItems(List<ReceiptItem> newItems) {
        this.items.clear();
        newItems.forEach(this::addItem);
    }

    public void markDuplicateSuspected(String newDedupeKey) {
        this.status = ReceiptStatus.DUPLICATE_SUSPECTED;
        this.dedupeKey = newDedupeKey;
    }

    /**
     * 점주가 업로드 내역에서 직접 수정할 때 사용한다. items는 orphanRemoval이 걸려있는
     * 컬렉션이라, 비우고 다시 채우면 Hibernate가 알아서 기존 항목 삭제 + 신규 항목 저장을 처리한다.
     */
    public void update(String documentType, String vendorName, String businessNumber,
                        LocalDate transactionDate, LocalTime transactionTime, String paymentMethod,
                        String category, Integer supplyAmount, Integer vat, Integer taxFreeAmount,
                        Integer totalAmount, String dedupeKey, List<ReceiptItem> newItems) {
        this.documentType = documentType;
        this.vendorName = vendorName;
        this.businessNumber = businessNumber;
        this.transactionDate = transactionDate;
        this.transactionTime = transactionTime;
        this.paymentMethod = paymentMethod;
        this.category = category;
        this.supplyAmount = supplyAmount;
        this.vat = vat;
        this.taxFreeAmount = taxFreeAmount == null ? 0 : taxFreeAmount;
        this.totalAmount = totalAmount;
        this.dedupeKey = dedupeKey;

        this.items.clear();
        for (ReceiptItem item : newItems) {
            addItem(item);
        }
    }
}
