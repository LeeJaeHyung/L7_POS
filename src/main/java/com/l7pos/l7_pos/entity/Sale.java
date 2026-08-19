package com.l7pos.l7_pos.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

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

    /**
     * 삭제되지 않은 품목만 담는다.
     *
     * SQLRestriction 덕분에 조회할 때마다 조건을 붙이지 않아도
     * 삭제된 품목은 이 목록에 들어오지 않는다.
     */
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLRestriction("deleted_at is null")
    private List<SaleItem> saleItems = new ArrayList<>();

    /** 삭제 시각. null 이면 살아있는 판매다. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

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

    /**
     * 판매 전체를 삭제 표시한다. 품목도 같은 시각으로 함께 표시한다.
     */
    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;

        for (SaleItem item : saleItems) {
            item.softDelete(deletedAt);
        }
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }
}