package com.l7pos.l7_pos.controller;

import com.l7pos.l7_pos.dto.SaleRow;
import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.service.SaleService;
import com.l7pos.l7_pos.util.BarcodeParser;
import com.l7pos.l7_pos.util.ParsedBarcode;
import com.l7pos.l7_pos.util.ProductNameUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

    private boolean busy = false;

    @FXML
    public void initialize() {
        runSafely("초기화 실패", () -> {
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
            clearBarcodeAndFocus();
        });
    }

    @FXML
    private void onAddBarcode() {
        if (busy) {
            return;
        }

        runSafely("상품 추가 실패", () -> {
            String barcode = barcodeField.getText();

            if (barcode == null || barcode.isBlank()) {
                throw new IllegalArgumentException("바코드를 입력하세요.");
            }

            addBarcodeAsync(barcode);
        });
    }

    private void addBarcodeAsync(String barcode) {
        setBusy(true);

        Task<SaleRow> task = new Task<>() {
            @Override
            protected SaleRow call() {
                ParsedBarcode parsed = BarcodeParser.parse(barcode);

                Product product = saleService.findProductByCode(parsed.productCode());

                if (product == null) {
                    throw new IllegalArgumentException("등록되지 않은 상품코드입니다: " + parsed.productCode());
                }

                String displayName = ProductNameUtil.toDisplayName(parsed.productCode());

                return new SaleRow(
                        parsed.barcode(),
                        parsed.productCode(),
                        displayName,
                        parsed.color(),
                        parsed.size(),
                        product.getPrice()
                );
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            saleRows.add(task.getValue());
            updateSummary();
            clearBarcodeAndFocus();
        });

        task.setOnFailed(event -> {
            setBusy(false);

            if (barcodeField != null) {
                barcodeField.requestFocus();
                barcodeField.selectAll();
            }

            handleTaskError("상품 추가 실패", task.getException());
        });

        startDaemonTask(task);
    }

    @FXML
    private void onDeleteSelected() {
        if (busy) {
            return;
        }

        runSafely("삭제 실패", () -> {
            SaleRow selected = saleTable.getSelectionModel().getSelectedItem();

            if (selected == null) {
                throw new IllegalArgumentException("삭제할 품목을 선택하세요.");
            }

            saleRows.remove(selected);
            updateSummary();
            clearBarcodeAndFocus();
        });
    }

    @FXML
    private void onRegisterSale() {
        if (busy) {
            return;
        }

        runSafely("등록 실패", () -> {
            if (saleRows.isEmpty()) {
                throw new IllegalArgumentException("등록할 판매 품목이 없습니다.");
            }

            String saleNo = saleNoField.getText();

            if (saleNo == null || saleNo.isBlank()) {
                throw new IllegalArgumentException("판매번호가 없습니다.");
            }

            ObservableList<SaleRow> snapshot = FXCollections.observableArrayList(saleRows);

            registerSaleAsync(saleNo, snapshot);
        });
    }

    private void registerSaleAsync(String saleNo, ObservableList<SaleRow> snapshot) {
        setBusy(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                saleService.saveSale(saleNo, snapshot);
                return saleService.createSaleNo();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            showInfo("등록 완료", "판매 등록이 완료되었습니다.");

            saleRows.clear();
            saleNoField.setText(task.getValue());
            updateSummary();
            clearBarcodeAndFocus();
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("등록 실패", task.getException());
            clearBarcodeAndFocus();
        });

        startDaemonTask(task);
    }

    @FXML
    private void onNewSale() {
        if (busy) {
            return;
        }

        runSafely("초기화 실패", () -> {
            boolean confirmed = true;

            if (!saleRows.isEmpty()) {
                confirmed = confirm(
                        "새 판매",
                        "현재 입력된 판매 품목이 삭제됩니다.\n새 판매를 시작하시겠습니까?"
                );
            }

            if (!confirmed) {
                return;
            }

            resetSale();
            clearBarcodeAndFocus();
        });
    }

    private void resetSale() {
        saleRows.clear();
        saleNoField.setText(saleService.createSaleNo());
        updateSummary();
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
        if (barcodeField != null) {
            barcodeField.clear();
            barcodeField.requestFocus();
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        if (barcodeField != null) {
            barcodeField.setDisable(busy);
        }

        if (saleTable != null) {
            saleTable.setDisable(busy);
        }
    }

    private void startDaemonTask(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleTaskError(String title, Throwable error) {
        if (error != null) {
            error.printStackTrace();
        }

        String message;

        if (error instanceof IllegalArgumentException) {
            message = error.getMessage();
        } else if (error != null && error.getMessage() != null && !error.getMessage().isBlank()) {
            message = "처리 중 오류가 발생했습니다.\n" + error.getMessage();
        } else {
            message = "처리 중 알 수 없는 오류가 발생했습니다.";
        }

        showWarning(title, message);
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        return alert.showAndWait()
                .filter(buttonType -> buttonType == ButtonType.OK)
                .isPresent();
    }

    private void runSafely(String errorTitle, Runnable action) {
        try {
            action.run();

        } catch (IllegalArgumentException e) {
            showWarning(errorTitle, e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            showWarning(errorTitle, "처리 중 오류가 발생했습니다.\n" + e.getMessage());
        }
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "알 수 없는 오류가 발생했습니다." : message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "" : message);
        alert.showAndWait();
    }
}