package com.l7pos.l7_pos.dto;

import javafx.beans.property.*;

public class SaleRow {

    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty productCode = new SimpleStringProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final StringProperty color = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final IntegerProperty price = new SimpleIntegerProperty();
    private final IntegerProperty amount = new SimpleIntegerProperty();

    public SaleRow(String barcode,
                   String productCode,
                   String productName,
                   String color,
                   String size,
                   int price) {
        this.barcode.set(barcode);
        this.productCode.set(productCode);
        this.productName.set(productName);
        this.color.set(color);
        this.size.set(size);
        this.price.set(price);
        this.amount.set(price);
    }

    public String getBarcode() {
        return barcode.get();
    }

    public String getProductCode() {
        return productCode.get();
    }

    public String getProductName() {
        return productName.get();
    }

    public String getColor() {
        return color.get();
    }

    public String getSize() {
        return size.get();
    }

    public int getPrice() {
        return price.get();
    }

    public int getAmount() {
        return amount.get();
    }

    public StringProperty barcodeProperty() {
        return barcode;
    }

    public StringProperty productCodeProperty() {
        return productCode;
    }

    public StringProperty productNameProperty() {
        return productName;
    }

    public StringProperty colorProperty() {
        return color;
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public IntegerProperty priceProperty() {
        return price;
    }

    public IntegerProperty amountProperty() {
        return amount;
    }
}