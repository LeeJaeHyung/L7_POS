package com.l7pos.l7_pos.controller;

import com.l7pos.l7_pos.dto.DeletedItemRow;
import com.l7pos.l7_pos.dto.ResolvedBarcode;
import com.l7pos.l7_pos.dto.SaleRow;
import com.l7pos.l7_pos.dto.SaleSummaryRow;
import com.l7pos.l7_pos.entity.Sale;
import com.l7pos.l7_pos.entity.SaleItem;
import com.l7pos.l7_pos.service.SaleService;
import com.l7pos.l7_pos.util.BarcodeParser;
import com.l7pos.l7_pos.util.EnglishInputGuard;
import com.l7pos.l7_pos.util.ParsedBarcode;
import com.l7pos.l7_pos.util.ProductNameUtil;
import com.l7pos.l7_pos.util.TableFormats;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class SaleHistoryController {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;

    @FXML private TextField productCodeSearchField;
    @FXML private Label productCodeSummaryLabel;

    @FXML private Label searchTotalQuantityLabel;
    @FXML private Label searchTotalAmountLabel;

    @FXML private TableView<SaleSummaryRow> saleTable;
    @FXML private TableColumn<SaleSummaryRow, String> saleNoColumn;
    @FXML private TableColumn<SaleSummaryRow, String> saleDateColumn;
    @FXML private TableColumn<SaleSummaryRow, Number> totalQuantityColumn;
    @FXML private TableColumn<SaleSummaryRow, Number> totalAmountColumn;
    @FXML private TableColumn<SaleSummaryRow, String> saleBarcodesColumn;

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

    /*
     * 품번 검색이 걸려 있는 상태를 기억한다.
     * 수정/삭제 후 목록을 다시 불러올 때 같은 조건으로 재조회하기 위함이다.
     * null 이면 품번 검색이 아닌 일반 조회 상태다.
     */
    private String activeProductCode = null;
    private LocalDate activeProductCodeStartDate = null;
    private LocalDate activeProductCodeEndDate = null;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static final DateTimeFormatter DATE_ONLY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 목록의 바코드 컬럼에서 바코드끼리 구분하는 문자 */
    private static final String BARCODE_SEPARATOR = ", ";

    @FXML
    public void initialize() {
        runSafely("초기화 실패", () -> {
            LocalDate today = LocalDate.now();

            applyDateFormat(startDatePicker);
            applyDateFormat(endDatePicker);

            startDatePicker.setValue(today);
            endDatePicker.setValue(today);

            initSaleTable();
            initItemTable();

            saleTable.setItems(saleRows);
            itemTable.setItems(itemRows);

            saleTable.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((obs, oldValue, newValue) -> {
                        if (internalSelectionChanging || busy) {
                            return;
                        }

                        if (newValue != null) {
                            loadSaleItemsAsync(newValue.getSaleNo());
                        }
                    });

            EnglishInputGuard.install(barcodeField);
            barcodeField.setOnAction(event -> onAddItem());

            // 한글 IME 를 우회해 항상 영문으로 입력받는다.
            EnglishInputGuard.install(productCodeSearchField);
            productCodeSearchField.setOnAction(event -> onSearchByProductCode());

            updateSearchSummary();
            updateDetailSummary();

            searchSalesByDateAsync(today, today, null);
        });
    }

    /**
     * 날짜 표시를 yyyy-MM-dd 로 통일한다.
     *
     * 기본값은 "2026. 8. 20." 처럼 나와서
     * 표의 판매일자(2026-08-20)와 형태가 달라 눈에 걸린다.
     * 직접 입력할 때도 같은 형식을 받는다.
     */
    private void applyDateFormat(DatePicker datePicker) {
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_ONLY_FORMATTER.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }

                return LocalDate.parse(text.trim(), DATE_ONLY_FORMATTER);
            }
        });

        datePicker.setPromptText("yyyy-MM-dd");
    }

    private void initSaleTable() {
        saleNoColumn.setCellValueFactory(data -> data.getValue().saleNoProperty());
        saleDateColumn.setCellValueFactory(data -> data.getValue().saleDateProperty());
        totalQuantityColumn.setCellValueFactory(data -> data.getValue().totalQuantityProperty());
        totalAmountColumn.setCellValueFactory(data -> data.getValue().totalAmountProperty());

        TableFormats.applyCount(totalQuantityColumn);
        TableFormats.applyMoney(totalAmountColumn);
        saleBarcodesColumn.setCellValueFactory(data -> data.getValue().barcodesProperty());

        /*
         * 바코드가 여러 개면 한 줄에 다 안 들어가므로
         * 셀에는 쉼표로 이어 붙여 보여주고
         * 마우스를 올리면 한 줄에 하나씩 전체를 보여준다.
         */
        saleBarcodesColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String barcodes, boolean empty) {
                super.updateItem(barcodes, empty);

                if (empty || barcodes == null || barcodes.isBlank()) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                setText(barcodes);
                setTooltip(new Tooltip(barcodes.replace(BARCODE_SEPARATOR, "\n")));
            }
        });
    }

    /**
     * 판매 한 건을 목록 행으로 변환한다.
     *
     * 바코드 컬럼에는 그 판매에 들어있는 바코드를 전부 담는다.
     * 같은 바코드가 여러 개 팔렸으면 팔린 개수만큼 그대로 나열한다.
     */
    private SaleSummaryRow toSummaryRow(Sale sale) {
        String barcodes = sale.getSaleItems()
                .stream()
                .map(SaleItem::getBarcode)
                .collect(Collectors.joining(BARCODE_SEPARATOR));

        return new SaleSummaryRow(
                sale.getSaleNo(),
                sale.getSaleDate().format(DATE_FORMATTER),
                sale.getTotalQuantity(),
                sale.getTotalAmount(),
                barcodes
        );
    }

    private void initItemTable() {
        barcodeColumn.setCellValueFactory(data -> data.getValue().barcodeProperty());
        productCodeColumn.setCellValueFactory(data -> data.getValue().productCodeProperty());
        productNameColumn.setCellValueFactory(data -> data.getValue().productNameProperty());
        colorColumn.setCellValueFactory(data -> data.getValue().colorProperty());
        sizeColumn.setCellValueFactory(data -> data.getValue().sizeProperty());
        priceColumn.setCellValueFactory(data -> data.getValue().priceProperty());
        amountColumn.setCellValueFactory(data -> data.getValue().amountProperty());

        TableFormats.applyMoney(priceColumn);
        TableFormats.applyMoney(amountColumn);
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

    /**
     * 품번 전체 기간 조회
     *
     * 컬러/사이즈는 무시하고 품번만으로 찾는다.
     * 바코드를 통째로 붙여넣어도 앞 10자리 품번만 사용한다.
     */
    @FXML
    private void onSearchByProductCode() {
        if (busy) {
            return;
        }

        runSafely("품번 조회 실패", () -> {
            String productCode = readProductCodeInput();

            searchSalesByProductCodeAsync(productCode, null, null, null);
        });
    }

    /**
     * 품번 + 기간 조회
     */
    @FXML
    private void onSearchByProductCodeInPeriod() {
        if (busy) {
            return;
        }

        runSafely("품번 기간 조회 실패", () -> {
            String productCode = readProductCodeInput();

            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            if (startDate == null || endDate == null) {
                throw new IllegalArgumentException("시작일과 종료일을 선택하세요.");
            }

            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }

            searchSalesByProductCodeAsync(productCode, startDate, endDate, null);
        });
    }

    /**
     * 입력창에서 검색할 품번을 읽는다.
     *
     * 엔터로 검색하면 입력창의 한글은 이미 영문으로 바뀌어 있지만,
     * 버튼 클릭이나 붙여넣기로 한글이 남아 있을 수 있어
     * toProductCode 안에서 한 번 더 변환한다.
     */
    private String readProductCodeInput() {
        String productCode = BarcodeParser.toProductCode(productCodeSearchField.getText());

        if (productCode.isEmpty()) {
            productCodeSearchField.requestFocus();
            throw new IllegalArgumentException("검색할 품번을 입력하세요.");
        }

        return productCode;
    }

    private void searchSalesByProductCodeAsync(String productCode,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               String selectSaleNoAfterLoad) {
        setBusy(true);

        Task<ProductCodeSearchResult> task = new Task<>() {
            @Override
            protected ProductCodeSearchResult call() {
                LocalDateTime startDateTime = startDate == null
                        ? null
                        : startDate.atStartOfDay();

                LocalDateTime endDateTime = endDate == null
                        ? null
                        : endDate.plusDays(1).atStartOfDay();

                List<Sale> sales = saleService.findSalesByProductCode(
                        productCode,
                        startDateTime,
                        endDateTime
                );

                List<SaleSummaryRow> rows = sales.stream()
                        .map(SaleHistoryController.this::toSummaryRow)
                        .toList();

                /*
                 * 판매 한 건에는 다른 품번도 섞여 있을 수 있으므로
                 * 검색한 품번에 해당하는 품목만 골라서 수량과 금액을 센다.
                 */
                int itemQuantity = 0;
                int itemAmount = 0;

                for (Sale sale : sales) {
                    for (SaleItem item : sale.getSaleItems()) {
                        if (matchesProductCode(item, productCode)) {
                            itemQuantity++;
                            itemAmount += item.getAmount();
                        }
                    }
                }

                return new ProductCodeSearchResult(
                        productCode,
                        rows,
                        itemQuantity,
                        itemAmount
                );
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            ProductCodeSearchResult result = task.getValue();

            activeProductCode = productCode;
            activeProductCodeStartDate = startDate;
            activeProductCodeEndDate = endDate;

            internalSelectionChanging = true;
            saleRows.setAll(result.sales());
            itemRows.clear();
            saleTable.getSelectionModel().clearSelection();
            itemTable.getSelectionModel().clearSelection();
            internalSelectionChanging = false;

            updateSearchSummary();
            updateDetailSummary();
            updateProductCodeSummary(result);

            if (result.sales().isEmpty()) {
                showInfo("품번 조회", "해당 품번의 판매 내역이 없습니다.\n품번: " + productCode);
                return;
            }

            if (selectSaleNoAfterLoad != null && !selectSaleNoAfterLoad.isBlank()) {
                selectSaleByNo(selectSaleNoAfterLoad);
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("품번 조회 실패", task.getException());
        });

        startDaemonTask(task);
    }

    /**
     * 품목이 검색한 품번에 해당하는지 확인한다.
     *
     * 10자리를 다 입력하면 정확히 일치하는 품번만,
     * 짧게 입력하면 앞자리가 일치하는 품번까지 포함된다.
     * (SaleService 의 LIKE 조건과 같은 기준)
     */
    private static boolean matchesProductCode(SaleItem item, String productCode) {
        String itemProductCode = item.getProductCode();

        return itemProductCode != null
                && itemProductCode.toUpperCase(Locale.ROOT).startsWith(productCode);
    }

    /**
     * 오늘 삭제된 판매 품목을 바코드 단위로 보여준다.
     *
     * 판매 전체를 지운 경우와 품목 하나만 뺀 경우가 모두 들어온다.
     */
    @FXML
    private void onShowDeletedToday() {
        if (busy) {
            return;
        }

        setBusy(true);

        Task<List<DeletedItemRow>> task = new Task<>() {
            @Override
            protected List<DeletedItemRow> call() {
                LocalDate today = LocalDate.now();

                return saleService.findDeletedItemsByDateRange(
                                today.atStartOfDay(),
                                today.plusDays(1).atStartOfDay()
                        )
                        .stream()
                        .map(item -> new DeletedItemRow(
                                item.getDeletedAt().format(TIME_FORMATTER),
                                item.getSale().getSaleNo(),
                                item.getBarcode(),
                                item.getProductCode(),
                                item.getColor(),
                                item.getSize(),
                                item.getAmount()
                        ))
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            showDeletedItemsDialog(task.getValue());
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("삭제 내역 조회 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void showDeletedItemsDialog(List<DeletedItemRow> rows) {
        Dialog<ButtonType> dialog = createBaseDialog("오늘 삭제 내역");

        Label titleLabel = new Label("오늘 삭제 내역");
        titleLabel.getStyleClass().add("section-title");

        int totalAmount = rows.stream().mapToInt(DeletedItemRow::getAmount).sum();

        Label summaryLabel = new Label(
                rows.isEmpty()
                        ? "오늘 삭제된 판매 품목이 없습니다."
                        : "바코드 " + rows.size() + "개 · "
                                + NumberFormat.getNumberInstance(Locale.KOREA).format(totalAmount) + "원"
        );
        summaryLabel.getStyleClass().add("page-desc");

        TableView<DeletedItemRow> table = new TableView<>();
        table.setPrefSize(900, 380);
        table.setPlaceholder(new Label("오늘 삭제된 판매 품목이 없습니다."));

        table.getColumns().addAll(
                textColumn("삭제 시각", 110, DeletedItemRow::deletedAtProperty),
                textColumn("판매번호", 190, DeletedItemRow::saleNoProperty),
                textColumn("바코드", 180, DeletedItemRow::barcodeProperty),
                textColumn("품번", 140, DeletedItemRow::productCodeProperty),
                textColumn("컬러", 70, DeletedItemRow::colorProperty),
                textColumn("사이즈", 70, DeletedItemRow::sizeProperty)
        );

        TableColumn<DeletedItemRow, Number> amountColumn = new TableColumn<>("금액");
        amountColumn.setPrefWidth(110);
        amountColumn.setCellValueFactory(data -> data.getValue().amountProperty());
        TableFormats.applyMoney(amountColumn);
        table.getColumns().add(amountColumn);

        table.getItems().setAll(rows);

        VBox contentBox = new VBox(12, titleLabel, summaryLabel, table);
        contentBox.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("닫기");
        }

        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private TableColumn<DeletedItemRow, String> textColumn(
            String title,
            double width,
            java.util.function.Function<DeletedItemRow, StringProperty> valueGetter) {

        TableColumn<DeletedItemRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> valueGetter.apply(data.getValue()));

        return column;
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
                        .map(SaleHistoryController.this::toSummaryRow)
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            internalSelectionChanging = true;
            saleRows.setAll(task.getValue());
            itemRows.clear();
            saleTable.getSelectionModel().clearSelection();
            itemTable.getSelectionModel().clearSelection();
            internalSelectionChanging = false;

            updateSearchSummary();
            updateDetailSummary();
            clearProductCodeSummary();

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
                        .map(SaleHistoryController.this::toSummaryRow)
                        .toList();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            internalSelectionChanging = true;
            saleRows.setAll(task.getValue());
            itemRows.clear();
            saleTable.getSelectionModel().clearSelection();
            itemTable.getSelectionModel().clearSelection();
            internalSelectionChanging = false;

            updateSearchSummary();
            updateDetailSummary();
            clearProductCodeSummary();

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
                // 앞에 잘못된 글자가 섞여 있어도 품번을 찾아낸다.
                ResolvedBarcode resolved = saleService.resolveBarcode(barcode);
                ParsedBarcode parsed = resolved.parsed();

                return new SaleRow(
                        parsed.barcode(),
                        parsed.productCode(),
                        ProductNameUtil.toDisplayName(parsed.productCode()),
                        parsed.color(),
                        parsed.size(),
                        resolved.product().getPrice()
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
            itemTable.getSelectionModel().clearSelection();
            itemTable.refresh();

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

            saleRows.removeIf(row ->
                    row != null &&
                            row.getSaleNo() != null &&
                            row.getSaleNo().equals(saleNo)
            );

            itemRows.clear();

            saleTable.getSelectionModel().clearSelection();
            itemTable.getSelectionModel().clearSelection();
            saleTable.refresh();
            itemTable.refresh();

            updateSearchSummary();
            updateDetailSummary();

            showInfo("삭제 완료", "판매 내역이 삭제되었습니다.");
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("삭제 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void reloadCurrentSearchAsync(String selectSaleNoAfterLoad) {
        // 품번 검색 중이었다면 같은 조건으로 다시 조회한다.
        if (activeProductCode != null) {
            searchSalesByProductCodeAsync(
                    activeProductCode,
                    activeProductCodeStartDate,
                    activeProductCodeEndDate,
                    selectSaleNoAfterLoad
            );
            return;
        }

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

        /*
         * 중요:
         * Alert/Dialog가 뜨거나 TableView 셀 이벤트 처리 중일 때
         * DatePicker, TableView, TextField를 disable 하면 macOS/JavaFX에서
         * 검은 화면 또는 UI가 사라지는 것처럼 보이는 현상이 생길 수 있다.
         *
         * 그래서 여기서는 상태 플래그만 변경하고 실제 컨트롤은 비활성화하지 않는다.
         */
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

    /**
     * 품번 검색 결과 요약
     *
     * 위쪽 "조회 총 수량 / 총 금액"은 검색된 판매 건 전체의 합계라
     * 다른 품번까지 포함된다.
     * 여기서는 검색한 품번만의 판매 수량과 금액을 따로 보여준다.
     */
    private void updateProductCodeSummary(ProductCodeSearchResult result) {
        productCodeSummaryLabel.setText(
                result.productCode()
                        + " · " + result.itemQuantity() + "개 · "
                        + NumberFormat.getNumberInstance(Locale.KOREA).format(result.itemAmount()) + "원"
                        + " (판매 " + result.sales().size() + "건)"
        );

        setProductCodeSummaryVisible(true);
    }

    /**
     * 요약 배지는 내용이 있을 때만 보여준다.
     * 빈 채로 두면 알약 모양 테두리만 남아 지저분하다.
     */
    private void setProductCodeSummaryVisible(boolean visible) {
        productCodeSummaryLabel.setVisible(visible);
        productCodeSummaryLabel.setManaged(visible);
    }

    private void clearProductCodeSummary() {
        activeProductCode = null;
        activeProductCodeStartDate = null;
        activeProductCodeEndDate = null;

        productCodeSummaryLabel.setText("");
        setProductCodeSummaryVisible(false);
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
        Dialog<ButtonType> dialog = createBaseDialog(title);

        Label titleLabel = new Label(title == null ? "확인" : title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.setWrapText(true);

        VBox contentBox = new VBox(12, titleLabel, messageLabel);
        contentBox.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("확인");
        }

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setText("취소");
        }

        Optional<ButtonType> result = dialog.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showWarning(String title, String message) {
        Dialog<ButtonType> dialog = createBaseDialog(title);

        Label titleLabel = new Label(title == null ? "알림" : title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label messageLabel = new Label(message == null ? "알 수 없는 오류가 발생했습니다." : message);
        messageLabel.setWrapText(true);

        VBox contentBox = new VBox(12, titleLabel, messageLabel);
        contentBox.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("확인");
        }

        dialog.showAndWait();
    }

    private void showInfo(String title, String message) {
        Dialog<ButtonType> dialog = createBaseDialog(title);

        Label titleLabel = new Label(title == null ? "알림" : title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label messageLabel = new Label(message == null ? "" : message);
        messageLabel.setWrapText(true);

        VBox contentBox = new VBox(12, titleLabel, messageLabel);
        contentBox.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("확인");
        }

        dialog.showAndWait();
    }

    private Dialog<ButtonType> createBaseDialog(String title) {
        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle(title == null ? "알림" : title);
        dialog.initModality(javafx.stage.Modality.WINDOW_MODAL);
        dialog.setResizable(false);

        Window owner = getOwnerWindow();

        if (owner != null) {
            dialog.initOwner(owner);
        }

        return dialog;
    }

    private Window getOwnerWindow() {
        if (saleTable != null && saleTable.getScene() != null) {
            return saleTable.getScene().getWindow();
        }

        if (itemTable != null && itemTable.getScene() != null) {
            return itemTable.getScene().getWindow();
        }

        if (barcodeField != null && barcodeField.getScene() != null) {
            return barcodeField.getScene().getWindow();
        }

        return null;
    }

    private record CopyBarcodeResult(String script, int count) {
    }

    private record ProductCodeSearchResult(String productCode,
                                           List<SaleSummaryRow> sales,
                                           int itemQuantity,
                                           int itemAmount) {
    }
}