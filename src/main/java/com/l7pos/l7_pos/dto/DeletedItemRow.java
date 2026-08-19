package com.l7pos.l7_pos.dto;

import javafx.beans.property.*;

/**
 * 삭제된 판매 품목 한 줄 (바코드 단위)
 *
 * 어느 판매번호에서 어떤 바코드가 언제 빠졌는지 보여준다.
 */
public class DeletedItemRow {

    private final StringProperty deletedAt = new SimpleStringProperty();
    private final StringProperty saleNo = new SimpleStringProperty();
    private final StringProperty barcode = new SimpleStringProperty();
    private final StringProperty productCode = new SimpleStringProperty();
    private final StringProperty color = new SimpleStringProperty();
    private final StringProperty size = new SimpleStringProperty();
    private final IntegerProperty amount = new SimpleIntegerProperty();

    public DeletedItemRow(String deletedAt,
                          String saleNo,
                          String barcode,
                          String productCode,
                          String color,
                          String size,
                          int amount) {
        this.deletedAt.set(deletedAt);
        this.saleNo.set(saleNo);
        this.barcode.set(barcode);
        this.productCode.set(productCode);
        this.color.set(color);
        this.size.set(size);
        this.amount.set(amount);
    }

    public String getBarcode() {
        return barcode.get();
    }

    public int getAmount() {
        return amount.get();
    }

    public StringProperty deletedAtProperty() {
        return deletedAt;
    }

    public StringProperty saleNoProperty() {
        return saleNo;
    }

    public StringProperty barcodeProperty() {
        return barcode;
    }

    public StringProperty productCodeProperty() {
        return productCode;
    }

    public StringProperty colorProperty() {
        return color;
    }

    public StringProperty sizeProperty() {
        return size;
    }

    public IntegerProperty amountProperty() {
        return amount;
    }
}
