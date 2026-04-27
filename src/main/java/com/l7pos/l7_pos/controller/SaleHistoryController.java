package com.l7pos.l7_pos.controller;

import com.l7pos.l7_pos.dto.SaleRow;
import com.l7pos.l7_pos.dto.SaleSummaryRow;
import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.entity.Sale;
import com.l7pos.l7_pos.entity.SaleItem;
import com.l7pos.l7_pos.service.SaleService;
import com.l7pos.l7_pos.util.BarcodeParser;
import com.l7pos.l7_pos.util.ParsedBarcode;
import com.l7pos.l7_pos.util.ProductNameUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SaleHistoryController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private Label searchTotalQuantityLabel;
    @FXML private Label searchTotalAmountLabel;

    @FXML private TableView<SaleSummaryRow> saleTable;
    @FXML private TableColumn<SaleSummaryRow, String> saleNoColumn;
    @FXML private TableColumn<SaleSummaryRow, String> saleDateColumn;
    @FXML private TableColumn<SaleSummaryRow, Number> totalQuantityColumn;
    @FXML private TableColumn<SaleSummaryRow, Number> totalAmountColumn;

    @FXML private TextField barcodeField;

    @FXML private TableView<SaleRow> itemTable;
    @FXML private TableColumn<SaleRow, String> barcodeColumn;
    @FXML private TableColumn<SaleRow, String> productCodeColumn;
    @FXML private TableColumn<SaleRow, String> productNameColumn;
    @FXML private TableColumn<SaleRow, String> colorColumn;
    @FXML private TableColumn<SaleRow, String> sizeColumn;
    @FXML private TableColumn<SaleRow, Number> priceColumn;
    @FXML private TableColumn<SaleRow, Number> amountColumn;

    @FXML private Label detailQuantityLabel;
    @FXML private Label detailAmountLabel;

    private final SaleService saleService = new SaleService();

    private final ObservableList<SaleSummaryRow> saleRows = FXCollections.observableArrayList();
    private final ObservableList<SaleRow> itemRows = FXCollections.observableArrayList();

    private boolean busy = false;
    private boolean internalSelectionChanging = false;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        runSafely("초기화 실패", () -> {
            LocalDate today = LocalDate.now();

            startDatePicker.setValue(today);
            endDatePicker.setValue(today);

            initSaleTable();
            initItemTable();

            saleTable.setItems(saleRows);
            itemTable.setItems(itemRows);

            saleTable.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((obs, oldValue, newValue) -> {
                        if (internalSelectionChanging) {
                            return;
                        }

                        if (newValue != null) {
                            loadSaleItemsAsync(newValue.getSaleNo());
                        }
                    });

            barcodeField.setOnAction(event -> onAddItem());

            updateSearchSummary();
            updateDetailSummary();

            searchSalesByDateAsync(today, today, null);
        });
    }

    private void initSaleTable() {
        saleNoColumn.setCellValueFactory(data -> data.getValue().saleNoProperty());
        saleDateColumn.setCellValueFactory(data -> data.getValue().saleDateProperty());
        totalQuantityColumn.setCellValueFactory(data -> data.getValue().totalQuantityProperty());
        totalAmountColumn.setCellValueFactory(data -> data.getValue().totalAmountProperty());
    }

    private void initItemTable() {
        barcodeColumn.setCellValueFactory(data -> data.getValue().barcodeProperty());
        productCodeColumn.setCellValueFactory(data -> data.getValue().productCodeProperty());
        productNameColumn.setCellValueFactory(data -> data.getValue().productNameProperty());
        colorColumn.setCellValueFactory(data -> data.getValue().colorProperty());
        sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty());
        amountColumn.setCellValueFactory(data -> data.getValue().amountProperty());
    }

    @FXML
    private void onReload() {
        if (busy) {
            return;
        }

        loadSalesAsync();
    }

    @FXML
    private void onSearchByDate() {
        if (busy) {
            return;
        }

        runSafely("조회 실패", () -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("시작일과 종료일을 선택하세요.");
            }

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }

            searchSalesByDateAsync(startDate, endDate, null);
        });
    }

    @FXML
    private void onCopyTodayBarcodeScript() {
        if (busy) {
            return;
        }

        setBusy(true);

        Task<CopyBarcodeResult> task = new Task<>() {
            @Override
            protected CopyBarcodeResult call() {
                LocalDate today = LocalDate.now();
                LocalDateTime startDateTime = today.atStartOfDay();
                LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();

                List<String> result = saleService.findBarcodesByDateRange(startDateTime, endDateTime);

                if (result == null || result.isEmpty()) {
                    throw new IllegalArgumentException("오늘 판매된 내역이 없습니다.");
                }

                List<String> barcodes = result.stream()
                        .filter(code -> code != null && !code.isBlank())
                        .map(code -> code.trim().toUpperCase())
                        .toList();

                if (barcodes.isEmpty()) {
                    throw new IllegalArgumentException("오늘 판매된 바코드가 없습니다.");
                }

                String script = generateUbiposConsoleScript(barcodes);

                return new CopyBarcodeResult(script, barcodes.size());
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            CopyBarcodeResult result = task.getValue();

            ClipboardContent content = new ClipboardContent();
            content.putString(result.script());

            boolean copied = Clipboard.getSystemClipboard().setContent(content);

            if (!copied) {
                showWarning("복사 실패", "클립보드에 복사하지 못했습니다.");
                return;
            }

            showInfo(
                    "복사 완료",
                    "오늘 판매된 바코드 " + result.count() + "건의 콘솔 스크립트가 복사되었습니다."
            );
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("복사 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private String generateUbiposConsoleScript(List<String> barcodes) {
        StringBuilder sb = new StringBuilder();

        sb.append("const input = $0;\n\n");

        sb.append("const barcodes = [\n");

        for (String barcode : barcodes) {
            sb.append("  \"")
                    .append(escapeJavascriptString(barcode))
                    .append("\",\n");
        }

        sb.append("];\n\n");

        sb.append("""
async function run() {
  for (const code of barcodes) {
    input.value = code;
    input.focus();

    window.event = {
      keyCode: 13,
      which: 13
    };

    EventChk2();

    console.log("처리:", code);
    await new Promise(r => setTimeout(r, 1500));
  }
}

run();
""");

        return sb.toString();
    }

    private String escapeJavascriptString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private void searchSalesByDateAsync(LocalDate startDate, LocalDate endDate, String selectSaleNoAfterLoad) {
        setBusy(true);

        Task<List<SaleSummaryRow>> task = new Task<>() {
            @Override
            protected List<SaleSummaryRow> call() {
                LocalDateTime startDateTime = startDate.atStartOfDay();
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

                return saleService.findSalesByDateRange(startDateTime, endDateTime)
                        .stream()
                        .map(sale -> new SaleSummaryRow(
                                sale.getSaleNo(),
                                sale.getSaleDate().format(DATE_FORMATTER),
                                sale.getTotalQuantity(),
                                sale.getTotalAmount()
                        ))
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            internalSelectionChanging = true;
            saleRows.setAll(task.getValue());
            itemRows.clear();
            saleTable.getSelectionModel().clearSelection();
            internalSelectionChanging = false;

            updateSearchSummary();
            updateDetailSummary();

            if (selectSaleNoAfterLoad != null && !selectSaleNoAfterLoad.isBlank()) {
                selectSaleByNo(selectSaleNoAfterLoad);
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("조회 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void loadSalesAsync() {
        setBusy(true);

        Task<List<SaleSummaryRow>> task = new Task<>() {
            @Override
            protected List<SaleSummaryRow> call() {
                return saleService.findAllSales()
                        .stream()
                        .map(sale -> new SaleSummaryRow(
                                sale.getSaleNo(),
                                sale.getSaleDate().format(DATE_FORMATTER),
                                sale.getTotalQuantity(),
                                sale.getTotalAmount()
                        ))
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            internalSelectionChanging = true;
            saleRows.setAll(task.getValue());
            itemRows.clear();
            saleTable.getSelectionModel().clearSelection();
            internalSelectionChanging = false;

            updateSearchSummary();
            updateDetailSummary();

            if (barcodeField != null) {
                barcodeField.clear();
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("전체 조회 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void loadSaleItemsAsync(String saleNo) {
        if (busy) {
            return;
        }

        if (saleNo == null || saleNo.isBlank()) {
            return;
        }

        setBusy(true);

        Task<List<SaleRow>> task = new Task<>() {
            @Override
            protected List<SaleRow> call() {
                Sale sale = saleService.findSaleWithItems(saleNo);

                return sale.getSaleItems()
                        .stream()
                        .map(item -> new SaleRow(
                                item.getBarcode(),
                                item.getProductCode(),
                                item.getProductName(),
                                item.getColor(),
                                item.getSize(),
                                item.getPrice()
                        ))
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            itemRows.setAll(task.getValue());
            updateDetailSummary();

            if (barcodeField != null) {
                barcodeField.requestFocus();
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("상세 조회 실패", task.getException());
        });

        startDaemonTask(task);
    }

    @FXML
    private void onAddItem() {
        if (busy) {
            return;
        }

        runSafely("품목 추가 실패", () -> {
            SaleSummaryRow selectedSale = saleTable.getSelectionModel().getSelectedItem();

            if (selectedSale == null) {
                throw new IllegalArgumentException("먼저 수정할 판매 내역을 선택하세요.");
            }

            String barcode = barcodeField.getText();

            if (barcode == null || barcode.isBlank()) {
                barcodeField.requestFocus();
                throw new IllegalArgumentException("바코드를 입력하세요.");
            }

            addItemAsync(barcode);
        });
    }

    private void addItemAsync(String barcode) {
        setBusy(true);

        Task<SaleRow> task = new Task<>() {
            @Override
            protected SaleRow call() {
                ParsedBarcode parsed = BarcodeParser.parse(barcode);

                Product product = saleService.findProductByCode(parsed.productCode());

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

            itemRows.add(task.getValue());

            if (barcodeField != null) {
                barcodeField.clear();
                barcodeField.requestFocus();
            }

            updateDetailSummary();
        });

        task.setOnFailed(event -> {
            setBusy(false);

            if (barcodeField != null) {
                barcodeField.requestFocus();
                barcodeField.selectAll();
            }

            handleTaskError("품목 추가 실패", task.getException());
        });

        startDaemonTask(task);
    }

    @FXML
    private void onDeleteItem() {
        if (busy) {
            return;
        }

        runSafely("삭제 실패", () -> {
            SaleRow selectedItem = itemTable.getSelectionModel().getSelectedItem();

            if (selectedItem == null) {
                throw new IllegalArgumentException("삭제할 품목을 선택하세요.");
            }

            itemRows.remove(selectedItem);
            updateDetailSummary();

            if (barcodeField != null) {
                barcodeField.requestFocus();
            }
        });
    }

    @FXML
    private void onUpdateSale() {
        if (busy) {
            return;
        }

        runSafely("수정 실패", () -> {
            SaleSummaryRow selectedSale = saleTable.getSelectionModel().getSelectedItem();

            if (selectedSale == null) {
                throw new IllegalArgumentException("수정할 판매 내역을 선택하세요.");
            }

            if (itemRows.isEmpty()) {
                throw new IllegalArgumentException("판매 품목이 0개입니다. 전체 삭제는 판매 삭제 버튼을 사용하세요.");
            }

            boolean confirmed = confirm(
                    "수정 저장",
                    "판매번호 [" + selectedSale.getSaleNo() + "] 내역을 수정 저장하시겠습니까?"
            );

            if (!confirmed) {
                return;
            }

            ObservableList<SaleRow> snapshot = FXCollections.observableArrayList(itemRows);

            updateSaleAsync(selectedSale.getSaleNo(), snapshot);
        });
    }

    private void updateSaleAsync(String saleNo, ObservableList<SaleRow> snapshot) {
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                saleService.updateSale(saleNo, snapshot);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            showInfo("수정 완료", "판매 내역이 수정되었습니다.");
            reloadCurrentSearchAsync(saleNo);
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("수정 실패", task.getException());
        });

        startDaemonTask(task);
    }

    @FXML
    private void onDeleteSale() {
        if (busy) {
            return;
        }

        runSafely("삭제 실패", () -> {
            SaleSummaryRow selectedSale = saleTable.getSelectionModel().getSelectedItem();

            if (selectedSale == null) {
                throw new IllegalArgumentException("삭제할 판매 내역을 선택하세요.");
            }

            boolean confirmed = confirm(
                    "판매 삭제",
                    "판매번호 [" + selectedSale.getSaleNo() + "]를 삭제하시겠습니까?\n상세 품목도 함께 삭제됩니다."
            );

            if (!confirmed) {
                return;
            }

            deleteSaleAsync(selectedSale.getSaleNo());
        });
    }

    private void deleteSaleAsync(String saleNo) {
        setBusy(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                saleService.deleteSale(saleNo);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            showInfo("삭제 완료", "판매 내역이 삭제되었습니다.");

            itemRows.clear();
            updateDetailSummary();

            reloadCurrentSearchAsync(null);
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("삭제 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void reloadCurrentSearchAsync(String selectSaleNoAfterLoad) {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            searchSalesByDateAsync(startDate, endDate, selectSaleNoAfterLoad);
        } else {
            loadSalesAsync();
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        if (startDatePicker != null) startDatePicker.setDisable(busy);
        if (endDatePicker != null) endDatePicker.setDisable(busy);
        if (saleTable != null) saleTable.setDisable(busy);
        if (itemTable != null) itemTable.setDisable(busy);
        if (barcodeField != null) barcodeField.setDisable(busy);
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

    private void selectSaleByNo(String saleNo) {
        if (saleNo == null || saleNo.isBlank()) {
            return;
        }

        for (SaleSummaryRow row : saleRows) {
            if (row.getSaleNo().equals(saleNo)) {
                saleTable.getSelectionModel().select(row);
                saleTable.scrollTo(row);
                return;
            }
        }
    }

    private void updateSearchSummary() {
        int totalQuantity = saleRows.stream()
                .mapToInt(SaleSummaryRow::getTotalQuantity)
                .sum();

        int totalAmount = saleRows.stream()
                .mapToInt(SaleSummaryRow::getTotalAmount)
                .sum();

        searchTotalQuantityLabel.setText(totalQuantity + "개");

        searchTotalAmountLabel.setText(
                NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount) + "원"
        );
    }

    private void updateDetailSummary() {
        int totalQuantity = itemRows.size();

        int totalAmount = itemRows.stream()
                .mapToInt(SaleRow::getAmount)
                .sum();

        detailQuantityLabel.setText(totalQuantity + "개");

        detailAmountLabel.setText(
                NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount) + "원"
        );
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
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

    private record CopyBarcodeResult(String script, int count) {
    }
}