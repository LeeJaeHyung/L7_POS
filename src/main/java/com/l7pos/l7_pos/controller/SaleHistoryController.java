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
                        if (newValue != null) {
                            loadSaleItems(newValue.getSaleNo());
                        }
                    });

            barcodeField.setOnAction(event -> onAddItem());

            searchSalesByDate(today, today);

            updateDetailSummary();
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
        runSafely("전체 조회 실패", () -> {
            loadSales();
            itemRows.clear();
            updateDetailSummary();
            updateSearchSummary();
            barcodeField.clear();
        });
    }

    @FXML
    private void onSearchByDate() {
        runSafely("조회 실패", () -> {
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("시작일과 종료일을 선택하세요.");
            }

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }

            searchSalesByDate(startDate, endDate);
        });
    }

    @FXML
    private void onCopyTodayBarcodeScript() {
        runSafely("복사 실패", () -> {
            LocalDate today = LocalDate.now();
            LocalDateTime startDateTime = today.atStartOfDay();
            LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();

            List<String> result = saleService.findBarcodesByDateRange(startDateTime, endDateTime);

            if (result == null || result.isEmpty()) {
                showInfo("알림", "오늘 판매된 내역이 없습니다.");
                return;
            }

            List<String> barcodes = result.stream()
                    .filter(code -> code != null && !code.isBlank())
                    .map(code -> code.trim().toUpperCase())
                    .toList();

            if (barcodes.isEmpty()) {
                showInfo("알림", "오늘 판매된 바코드가 없습니다.");
                return;
            }

            String script = generateUbiposConsoleScript(barcodes);

            ClipboardContent content = new ClipboardContent();
            content.putString(script);

            boolean copied = Clipboard.getSystemClipboard().setContent(content);

            if (!copied) {
                throw new IllegalArgumentException("클립보드에 복사하지 못했습니다.");
            }

            showInfo("복사 완료", "오늘 판매된 바코드 " + barcodes.size() + "건의 콘솔 스크립트가 복사되었습니다.");
        });
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

    private void searchSalesByDate(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        saleRows.clear();
        itemRows.clear();

        for (Sale sale : saleService.findSalesByDateRange(startDateTime, endDateTime)) {
            saleRows.add(new SaleSummaryRow(
                    sale.getSaleNo(),
                    sale.getSaleDate().format(DATE_FORMATTER),
                    sale.getTotalQuantity(),
                    sale.getTotalAmount()
            ));
        }

        updateSearchSummary();
        updateDetailSummary();
    }

    private void loadSales() {
        saleRows.clear();

        for (Sale sale : saleService.findAllSales()) {
            saleRows.add(new SaleSummaryRow(
                    sale.getSaleNo(),
                    sale.getSaleDate().format(DATE_FORMATTER),
                    sale.getTotalQuantity(),
                    sale.getTotalAmount()
            ));
        }

        updateSearchSummary();
    }

    private void loadSaleItems(String saleNo) {
        runSafely("상세 조회 실패", () -> {
            itemRows.clear();

            Sale sale = saleService.findSaleWithItems(saleNo);

            for (SaleItem item : sale.getSaleItems()) {
                itemRows.add(new SaleRow(
                        item.getBarcode(),
                        item.getProductCode(),
                        item.getProductName(),
                        item.getColor(),
                        item.getSize(),
                        item.getPrice()
                ));
            }

            updateDetailSummary();
        });
    }

    @FXML
    private void onAddItem() {
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

            ParsedBarcode parsed = BarcodeParser.parse(barcode);

            Product product = saleService.findProductByCode(parsed.productCode());

            String displayName = ProductNameUtil.toDisplayName(parsed.productCode());

            itemRows.add(new SaleRow(
                    parsed.barcode(),
                    parsed.productCode(),
                    displayName,
                    parsed.color(),
                    parsed.size(),
                    product.getPrice()
            ));

            barcodeField.clear();
            barcodeField.requestFocus();
            updateDetailSummary();
        });
    }

    @FXML
    private void onDeleteItem() {
        runSafely("삭제 실패", () -> {
            SaleRow selectedItem = itemTable.getSelectionModel().getSelectedItem();

            if (selectedItem == null) {
                throw new IllegalArgumentException("삭제할 품목을 선택하세요.");
            }

            itemRows.remove(selectedItem);
            updateDetailSummary();
            barcodeField.requestFocus();
        });
    }

    @FXML
    private void onUpdateSale() {
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

            saleService.updateSale(selectedSale.getSaleNo(), itemRows);

            showInfo("수정 완료", "판매 내역이 수정되었습니다.");

            reloadCurrentSearchSafely();
            selectSaleByNo(selectedSale.getSaleNo());
        });
    }

    @FXML
    private void onDeleteSale() {
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

            saleService.deleteSale(selectedSale.getSaleNo());

            showInfo("삭제 완료", "판매 내역이 삭제되었습니다.");

            reloadCurrentSearchSafely();
            itemRows.clear();
            updateDetailSummary();
        });
    }

    private void reloadCurrentSearchSafely() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
            searchSalesByDate(startDate, endDate);
        } else {
            loadSales();
        }
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