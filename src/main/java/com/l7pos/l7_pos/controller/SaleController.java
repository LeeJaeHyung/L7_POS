package com.l7pos.l7_pos.controller;

import com.l7pos.l7_pos.dto.SaleRow;
import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.service.SaleService;
import com.l7pos.l7_pos.util.BarcodeParser;
import com.l7pos.l7_pos.util.ParsedBarcode;
import com.l7pos.l7_pos.util.ProductNameUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.NumberFormat;
import java.util.Locale;

public class SaleController {

    @FXML private TextField saleNoField;
    @FXML private TextField barcodeField;

    @FXML private TableView<SaleRow> saleTable;
    @FXML private TableColumn<SaleRow, String> barcodeColumn;
    @FXML private TableColumn<SaleRow, String> productCodeColumn;
    @FXML private TableColumn<SaleRow, String> productNameColumn;
    @FXML private TableColumn<SaleRow, String> colorColumn;
    @FXML private TableColumn<SaleRow, String> sizeColumn;
    @FXML private TableColumn<SaleRow, Number> priceColumn;
    @FXML private TableColumn<SaleRow, Number> amountColumn;

    @FXML private Label totalQuantityLabel;
    @FXML private Label totalAmountLabel;

    private final ObservableList<SaleRow> saleRows = FXCollections.observableArrayList();
    private final SaleService saleService = new SaleService();

    @FXML
    public void initialize() {
        saleNoField.setText(saleService.createSaleNo());

        saleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        barcodeColumn.setCellValueFactory(data -> data.getValue().barcodeProperty());
        productCodeColumn.setCellValueFactory(data -> data.getValue().productCodeProperty());
        productNameColumn.setCellValueFactory(data -> data.getValue().productNameProperty());
        colorColumn.setCellValueFactory(data -> data.getValue().colorProperty());
        sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty());
        amountColumn.setCellValueFactory(data -> data.getValue().amountProperty());

        saleTable.setItems(saleRows);

        barcodeField.setOnAction(event -> onAddBarcode());

        updateSummary();

        barcodeField.requestFocus();
    }

    @FXML
    private void onAddBarcode() {
        String barcode = barcodeField.getText();

        if (barcode == null || barcode.isBlank()) {
            showWarning("입력 오류", "바코드를 입력하세요.");
            clearBarcodeAndFocus();
            return;
        }

        try {
            ParsedBarcode parsed = BarcodeParser.parse(barcode);

            Product product = saleService.findProductByCode(parsed.productCode());

            String displayName = ProductNameUtil.toDisplayName(parsed.productCode());

            SaleRow row = new SaleRow(
                    parsed.barcode(),
                    parsed.productCode(),
                    displayName,
                    parsed.color(),
                    parsed.size(),
                    product.getPrice()
            );

            saleRows.add(row);
            updateSummary();

        } catch (IllegalArgumentException e) {
            showWarning("상품 추가 실패", e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("시스템 오류", "상품 처리 중 오류가 발생했습니다.");
        }

        clearBarcodeAndFocus();
    }

    @FXML
    private void onDeleteSelected() {
        SaleRow selected = saleTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("삭제 실패", "삭제할 품목을 선택하세요.");
            clearBarcodeAndFocus();
            return;
        }

        saleRows.remove(selected);
        updateSummary();

        clearBarcodeAndFocus();
    }

    @FXML
    private void onRegisterSale() {
        try {
            if (saleRows.isEmpty()) {
                showWarning("등록 실패", "등록할 판매 품목이 없습니다.");
                clearBarcodeAndFocus();
                return;
            }

            saleService.saveSale(saleNoField.getText(), saleRows);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("등록 완료");
            alert.setHeaderText(null);
            alert.setContentText("판매 등록이 완료되었습니다.");
            alert.showAndWait();

            saleRows.clear();
            saleNoField.setText(saleService.createSaleNo());
            updateSummary();
            clearBarcodeAndFocus();

        } catch (IllegalArgumentException e) {
            showWarning("등록 실패", e.getMessage());
            clearBarcodeAndFocus();

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("시스템 오류", "판매 등록 중 오류가 발생했습니다.");
            clearBarcodeAndFocus();
        }
    }

    @FXML
    private void onNewSale() {
        resetSale();
    }

    private void resetSale() {
        saleRows.clear();
        saleNoField.setText(saleService.createSaleNo());
        updateSummary();
        clearBarcodeAndFocus();
    }

    private void updateSummary() {
        int totalQuantity = saleRows.size();

        int totalAmount = saleRows.stream()
                .mapToInt(SaleRow::getAmount)
                .sum();

        totalQuantityLabel.setText(totalQuantity + "개");

        totalAmountLabel.setText(
                NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount) + "원"
        );
    }

    private void clearBarcodeAndFocus() {
        barcodeField.clear();
        barcodeField.requestFocus();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}