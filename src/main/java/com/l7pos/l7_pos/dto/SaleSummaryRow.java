package com.l7pos.l7_pos.dto;

import javafx.beans.property.*;

public class SaleSummaryRow {

    private final StringProperty saleNo = new SimpleStringProperty();
    private final StringProperty saleDate = new SimpleStringProperty();
    private final IntegerProperty totalQuantity = new SimpleIntegerProperty();
    private final IntegerProperty totalAmount = new SimpleIntegerProperty();

    /** 이 판매에 포함된 바코드 전체 (목록에 한 줄로 표시) */
    private final StringProperty barcodes = new SimpleStringProperty();

    public SaleSummaryRow(String saleNo,
                          String saleDate,
                          int totalQuantity,
                          int totalAmount,
                          String barcodes) {
        this.saleNo.set(saleNo);
        this.saleDate.set(saleDate);
        this.totalQuantity.set(totalQuantity);
        this.totalAmount.set(totalAmount);
        this.barcodes.set(barcodes);
    }

    public String getSaleNo() {
        return saleNo.get();
    }

    public String getSaleDate() {
        return saleDate.get();
    }

    public int getTotalQuantity() {
        return totalQuantity.get();
    }

    public int getTotalAmount() {
        return totalAmount.get();
    }

    public String getBarcodes() {
        return barcodes.get();
    }

    public StringProperty saleNoProperty() {
        return saleNo;
    }

    public StringProperty saleDateProperty() {
        return saleDate;
    }

    public IntegerProperty totalQuantityProperty() {
        return totalQuantity;
    }

    public IntegerProperty totalAmountProperty() {
        return totalAmount;
    }

    public StringProperty barcodesProperty() {
        return barcodes;
    }
}