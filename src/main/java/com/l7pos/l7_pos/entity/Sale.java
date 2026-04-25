package com.l7pos.l7_pos.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sale")
public class Sale {

    @Id
    @Column(name = "sale_no", length = 30)
    private String saleNo;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "total_amount", nullable = false)
    private int totalAmount;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> saleItems = new ArrayList<>();

    public Sale() {}

    public Sale(String saleNo, LocalDateTime saleDate, int totalQuantity, int totalAmount) {
        this.saleNo = saleNo;
        this.saleDate = saleDate;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    public void addItem(SaleItem item) {
        saleItems.add(item);
        item.setSale(this);
    }

    public void clearItems() {
        saleItems.clear();
    }

    public String getSaleNo() {
        return saleNo;
    }

    public LocalDateTime getSaleDate() {
        return saleDate;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public List<SaleItem> getSaleItems() {
        return saleItems;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }
}