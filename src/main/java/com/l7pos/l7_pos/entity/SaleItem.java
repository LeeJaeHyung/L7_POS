package com.l7pos.l7_pos.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sale_item")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id")
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_no", nullable = false)
    private Sale sale;

    @Column(name = "barcode", nullable = false, length = 50)
    private String barcode;

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "color", nullable = false, length = 10)
    private String color;

    @Column(name = "size", nullable = false, length = 10)
    private String size;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "amount", nullable = false)
    private int amount;

    /**
     * 삭제 시각. null 이면 살아있는 품목이다.
     *
     * 판매 기록은 지우지 않고 표시만 남긴다.
     * 언제 무엇을 뺐는지 나중에 확인할 수 있어야 하기 때문이다.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public SaleItem() {}

    public SaleItem(String barcode,
                    String productCode,
                    String productName,
                    String color,
                    String size,
                    int price) {
        this.barcode = barcode;
        this.productCode = productCode;
        this.productName = productName;
        this.color = color;
        this.size = size;
        this.price = price;
        this.amount = price;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Sale getSale() {
        return sale;
    }

    public Long getSaleItemId() {
        return saleItemId;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getProductName() {
        return productName;
    }

    public String getColor() {
        return color;
    }

    public String getSize() {
        return size;
    }

    public int getPrice() {
        return price;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}