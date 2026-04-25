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

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

        // 온로딩 시 오늘 날짜 기준 조회
        searchSalesByDate(today, today);

        updateDetailSummary();
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
        loadSales();
        itemRows.clear();
        updateDetailSummary();
        updateSearchSummary();
        barcodeField.clear();
    }

    @FXML
    private void onSearchByDate() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            showWarning("조회 실패", "시작일과 종료일을 선택하세요.");
            return;
        }

        if (startDate.isAfter(endDate)) {
            showWarning("조회 실패", "시작일은 종료일보다 늦을 수 없습니다.");
            return;
        }

        searchSalesByDate(startDate, endDate);
    }

    private void searchSalesByDate(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        saleRows.clear();
        itemRows.clear();

        try {
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

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("조회 실패", "판매 내역 조회 중 오류가 발생했습니다.");
        }
    }

    private void loadSales() {
        saleRows.clear();

        try {
            for (Sale sale : saleService.findAllSales()) {
                saleRows.add(new SaleSummaryRow(
                        sale.getSaleNo(),
                        sale.getSaleDate().format(DATE_FORMATTER),
                        sale.getTotalQuantity(),
                        sale.getTotalAmount()
                ));
            }

            updateSearchSummary();

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("조회 실패", "판매 내역 조회 중 오류가 발생했습니다.");
        }
    }

    private void loadSaleItems(String saleNo) {
        itemRows.clear();

        try {
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

        } catch (Exception e) {
            showWarning("상세 조회 실패", e.getMessage());
        }
    }

    @FXML
    private void onAddItem() {
        SaleSummaryRow selectedSale = saleTable.getSelectionModel().getSelectedItem();

        if (selectedSale == null) {
            showWarning("추가 실패", "먼저 수정할 판매 내역을 선택하세요.");
            return;
        }

        String barcode = barcodeField.getText();

        if (barcode == null || barcode.isBlank()) {
            showWarning("입력 오류", "바코드를 입력하세요.");
            barcodeField.requestFocus();
            return;
        }

        try {
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

        } catch (IllegalArgumentException e) {
            showWarning("품목 추가 실패", e.getMessage());
            barcodeField.clear();
            barcodeField.requestFocus();

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("시스템 오류", "품목 추가 중 오류가 발생했습니다.");
            barcodeField.clear();
            barcodeField.requestFocus();
        }
    }

    @FXML
    private void onDeleteItem() {
        SaleRow selectedItem = itemTable.getSelectionModel().getSelectedItem();

        if (selectedItem == null) {
            showWarning("삭제 실패", "삭제할 품목을 선택하세요.");
            return;
        }

        itemRows.remove(selectedItem);
        updateDetailSummary();
        barcodeField.requestFocus();
    }

    @FXML
    private void onUpdateSale() {
        SaleSummaryRow selectedSale = saleTable.getSelectionModel().getSelectedItem();

        if (selectedSale == null) {
            showWarning("수정 실패", "수정할 판매 내역을 선택하세요.");
            return;
        }

        if (itemRows.isEmpty()) {
            showWarning("수정 실패", "판매 품목이 0개입니다. 전체 삭제는 판매 삭제 버튼을 사용하세요.");
            return;
        }

        boolean confirmed = confirm(
                "수정 저장",
                "판매번호 [" + selectedSale.getSaleNo() + "] 내역을 수정 저장하시겠습니까?"
        );

        if (!confirmed) {
            return;
        }

        try {
            saleService.updateSale(selectedSale.getSaleNo(), itemRows);

            showInfo("수정 완료", "판매 내역이 수정되었습니다.");

            onSearchByDate();
            selectSaleByNo(selectedSale.getSaleNo());

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("수정 실패", e.getMessage());
        }
    }

    @FXML
    private void onDeleteSale() {
        SaleSummaryRow selectedSale = saleTable.getSelectionModel().getSelectedItem();

        if (selectedSale == null) {
            showWarning("삭제 실패", "삭제할 판매 내역을 선택하세요.");
            return;
        }

        boolean confirmed = confirm(
                "판매 삭제",
                "판매번호 [" + selectedSale.getSaleNo() + "]를 삭제하시겠습니까?\n상세 품목도 함께 삭제됩니다."
        );

        if (!confirmed) {
            return;
        }

        try {
            saleService.deleteSale(selectedSale.getSaleNo());

            showInfo("삭제 완료", "판매 내역이 삭제되었습니다.");

            onSearchByDate();
            itemRows.clear();
            updateDetailSummary();

        } catch (Exception e) {
            e.printStackTrace();
            showWarning("삭제 실패", e.getMessage());
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