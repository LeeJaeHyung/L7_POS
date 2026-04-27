package com.l7pos.l7_pos.controller;

import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.repository.ProductRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProductRegisterController {

    @FXML private TextField barcodeField;
    @FXML private TextField priceField;
    @FXML private TextField searchField;

    @FXML private Button registerButton;
    @FXML private Button cancelEditButton;
    @FXML private Button searchButton;
    @FXML private Button resetButton;

    @FXML private Label messageLabel;

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Long> idColumn;
    @FXML private TableColumn<Product, String> barcodeColumn;
    @FXML private TableColumn<Product, Integer> priceColumn;
    @FXML private TableColumn<Product, Product> actionColumn;

    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final ProductRepository productRepository = new ProductRepository();

    private Product editingProduct;

    @FXML
    public void initialize() {
        runSafely("초기화 실패", () -> {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
            barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
            priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

            priceColumn.setCellFactory(column -> new TableCell<>() {
                private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : numberFormat.format(item) + " 원");
                }
            });

            actionColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            actionColumn.setCellFactory(column -> new TableCell<>() {
                private final Button editButton = new Button("수정");
                private final Button deleteButton = new Button("삭제");
                private final HBox buttonBox = new HBox(8, editButton, deleteButton);

                {
                    editButton.getStyleClass().add("table-edit-button");
                    deleteButton.getStyleClass().add("table-delete-button");

                    editButton.setOnAction(event -> {
                        Product product = getTableRow().getItem();
                        if (product != null) {
                            startEditProduct(product);
                        }
                    });

                    deleteButton.setOnAction(event -> {
                        Product product = getTableRow().getItem();
                        if (product != null) {
                            deleteProduct(product);
                        }
                    });
                }

                @Override
                protected void updateItem(Product item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty || item == null ? null : buttonBox);
                }
            });

            productTable.setItems(productList);

            priceField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    priceField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });

            barcodeField.setOnAction(event -> priceField.requestFocus());
            priceField.setOnAction(event -> onSave());
            searchField.setOnAction(event -> onSearch());

            loadProductsAsync("전체 상품 목록을 조회했습니다.");
            updateFormState();
            barcodeField.requestFocus();
        });
    }

    @FXML
    private void onSave() {
        runSafely("저장 실패", () -> {
            String barcode = barcodeField.getText() == null ? "" : barcodeField.getText().trim();
            String priceText = priceField.getText() == null ? "" : priceField.getText().trim();

            if (barcode.isEmpty()) {
                barcodeField.requestFocus();
                throw new IllegalArgumentException("바코드를 입력해주세요.");
            }

            if (priceText.isEmpty()) {
                priceField.requestFocus();
                throw new IllegalArgumentException("가격을 입력해주세요.");
            }

            int price;
            try {
                price = Integer.parseInt(priceText);
            } catch (NumberFormatException e) {
                priceField.requestFocus();
                priceField.selectAll();
                throw new IllegalArgumentException("가격은 숫자만 입력 가능합니다.");
            }

            if (price <= 0) {
                priceField.requestFocus();
                priceField.selectAll();
                throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
            }

            String productCode = extractProductCode(barcode);

            if (editingProduct == null) {
                registerProduct(productCode, price);
            } else {
                updateProduct(productCode, price);
            }
        });
    }

    @FXML
    private void onSearch() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();

        if (keyword.isEmpty()) {
            loadProductsAsync("전체 상품 목록을 조회했습니다.");
            return;
        }

        if (!keyword.matches("^[A-Za-z0-9]+$")) {
            showMessage("검색어는 영문과 숫자만 입력 가능합니다.", false);
            searchField.requestFocus();
            searchField.selectAll();
            return;
        }

        if (keyword.length() < 2) {
            showMessage("검색어는 2글자 이상 입력하세요.", false);
            searchField.requestFocus();
            searchField.selectAll();
            return;
        }

        setBusy(true);
        showMessage("조회 중입니다...", true);

        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() {
                return productRepository.searchByProductCode(keyword);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            List<Product> products = task.getValue();
            productList.setAll(products);

            if (products == null || products.isEmpty()) {
                showMessage("조회 결과가 없습니다.", false);
            } else {
                showMessage(products.size() + "건 조회되었습니다.", true);
            }

            searchField.requestFocus();
        });

        task.setOnFailed(event -> {
            setBusy(false);

            Throwable error = task.getException();
            if (error != null) {
                error.printStackTrace();
            }

            showMessage("조회 중 오류가 발생했습니다.", false);
            searchField.requestFocus();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onReset() {
        runSafely("초기화 실패", () -> {
            searchField.clear();
            clearForm();
            loadProductsAsync("전체 상품 목록으로 초기화되었습니다.");
        });
    }

    @FXML
    private void onCancelEdit() {
        runSafely("수정 취소 실패", () -> {
            clearForm();
            showMessage("수정 모드가 취소되었습니다.", true);
        });
    }

    private void registerProduct(String productCode, int price) {
        if (productRepository.existsByProductCode(productCode)) {
            barcodeField.requestFocus();
            barcodeField.selectAll();
            throw new IllegalArgumentException("이미 등록된 상품코드입니다.");
        }

        Product product = new Product(productCode, price);
        productRepository.save(product);

        clearForm();
        loadProductsAsync("상품이 등록되었습니다. [상품코드: " + productCode + "]");
    }

    private void updateProduct(String productCode, int price) {
        if (editingProduct == null) {
            throw new IllegalArgumentException("수정할 상품이 선택되지 않았습니다.");
        }

        if (!editingProduct.getProductCode().equals(productCode)) {
            barcodeField.setText(editingProduct.getProductCode());
            barcodeField.requestFocus();
            barcodeField.selectAll();
            throw new IllegalArgumentException("수정 시 상품코드는 변경할 수 없습니다.");
        }

        Long editingId = editingProduct.getId();
        productRepository.update(editingId, price);

        clearForm();
        loadProductsAsync("상품이 수정되었습니다. [ID: " + editingId + "]");
    }

    private void startEditProduct(Product product) {
        runSafely("수정 모드 전환 실패", () -> {
            if (product == null) {
                throw new IllegalArgumentException("수정할 상품이 없습니다.");
            }

            editingProduct = product;
            barcodeField.setText(product.getProductCode());
            priceField.setText(String.valueOf(product.getPrice()));
            barcodeField.setDisable(true);
            registerButton.setText("상품 수정");
            cancelEditButton.setVisible(true);
            cancelEditButton.setManaged(true);
            priceField.requestFocus();
            priceField.selectAll();
            showMessage("수정할 상품을 불러왔습니다. 가격을 수정 후 저장하세요.", true);
        });
    }

    private void deleteProduct(Product product) {
        runSafely("상품 삭제 실패", () -> {
            if (product == null) {
                throw new IllegalArgumentException("삭제할 상품이 없습니다.");
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("상품 삭제");
            alert.setHeaderText("선택한 상품을 삭제하시겠습니까?");
            alert.setContentText("ID: " + product.getId() + " / 상품코드: " + product.getProductCode());

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }

            Long deleteId = product.getId();
            productRepository.deleteById(deleteId);

            if (editingProduct != null && editingProduct.getId().equals(deleteId)) {
                clearForm();
            }

            loadProductsAsync("상품이 삭제되었습니다. [ID: " + deleteId + "]");
        });
    }

    private String extractProductCode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("바코드를 입력해주세요.");
        }

        String normalized = barcode.trim().toUpperCase();

        if (!normalized.matches("^[A-Z0-9]+$")) {
            throw new IllegalArgumentException("바코드는 영문과 숫자만 입력 가능합니다.");
        }

        if (normalized.length() < 10) {
            throw new IllegalArgumentException("바코드 형식이 올바르지 않습니다. 최소 10자리 이상이어야 합니다.");
        }

        return normalized.substring(0, 10).toLowerCase();
    }

    private void loadProductsAsync(String successMessage) {
        setBusy(true);
        showMessage("상품 목록 조회 중입니다...", true);

        Task<List<Product>> task = new Task<>() {
            @Override
            protected List<Product> call() {
                return productRepository.findAll();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            List<Product> products = task.getValue();
            productList.setAll(products);

            if (successMessage != null && !successMessage.isBlank()) {
                showMessage(successMessage, true);
            }

            if (barcodeField != null && !barcodeField.isDisabled()) {
                barcodeField.requestFocus();
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);

            Throwable error = task.getException();
            if (error != null) {
                error.printStackTrace();
            }

            showMessage("상품 목록 조회 중 오류가 발생했습니다.", false);

            if (barcodeField != null && !barcodeField.isDisabled()) {
                barcodeField.requestFocus();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void clearForm() {
        editingProduct = null;
        barcodeField.clear();
        priceField.clear();
        barcodeField.setDisable(false);
        productTable.getSelectionModel().clearSelection();
        updateFormState();
        barcodeField.requestFocus();
    }

    private void updateFormState() {
        boolean editMode = editingProduct != null;
        registerButton.setText(editMode ? "상품 수정" : "상품 등록");
        cancelEditButton.setVisible(editMode);
        cancelEditButton.setManaged(editMode);
    }

    private void setBusy(boolean busy) {
        if (searchButton != null) searchButton.setDisable(busy);
        if (resetButton != null) resetButton.setDisable(busy);
        if (registerButton != null) registerButton.setDisable(busy);
        if (searchField != null) searchField.setDisable(busy);
        if (productTable != null) productTable.setDisable(busy);
    }

    private void runSafely(String errorTitle, Runnable action) {
        try {
            action.run();

        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), false);

        } catch (Exception e) {
            e.printStackTrace();
            showMessage(errorTitle + " - 처리 중 오류가 발생했습니다.\n" + e.getMessage(), false);

        } finally {
            if (barcodeField != null && !barcodeField.isDisabled()) {
                barcodeField.requestFocus();
            }
        }
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message == null ? "알 수 없는 오류가 발생했습니다." : message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");

        if (success) {
            messageLabel.getStyleClass().add("message-success");
        } else {
            messageLabel.getStyleClass().add("message-error");
        }

        updateFormState();
    }
}