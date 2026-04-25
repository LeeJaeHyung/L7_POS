package com.l7pos.l7_pos.entity;

import jakarta.persistence.*;

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
}