package com.l7pos.l7_pos.dto;

import javafx.beans.property.*;

public class SaleSummaryRow {

    private final StringProperty saleNo = new SimpleStringProperty();
    private final StringProperty saleDate = new SimpleStringProperty();
    private final IntegerProperty totalQuantity = new SimpleIntegerProperty();
    private final IntegerProperty totalAmount = new SimpleIntegerProperty();

    public SaleSummaryRow(String saleNo, String saleDate, int totalQuantity, int totalAmount) {
        this.saleNo.set(saleNo);
        this.saleDate.set(saleDate);
        this.totalQuantity.set(totalQuantity);
        this.totalAmount.set(totalAmount);
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
}