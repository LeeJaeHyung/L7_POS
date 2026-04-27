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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

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
    private boolean busy = false;

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
                        if (busy) {
                            return;
                        }

                        Product product = getTableRow().getItem();

                        if (product != null) {
                            startEditProduct(product);
                        }
                    });

                    deleteButton.setOnAction(event -> {
                        if (busy) {
                            return;
                        }

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
                if (newValue != null && !newValue.matches("\\d*")) {
                    priceField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });

            barcodeField.setOnAction(event -> priceField.requestFocus());
            priceField.setOnAction(event -> onSave());
            searchField.setOnAction(event -> onSearch());

            updateFormState();
            loadProductsAsync("전체 상품 목록을 조회했습니다.");
        });
    }

    @FXML
    private void onSave() {
        if (busy) {
            return;
        }

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
                registerProductAsync(productCode, price);
            } else {
                updateProductAsync(productCode, price);
            }
        });
    }

    @FXML
    private void onSearch() {
        if (busy) {
            return;
        }

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

        searchProductsAsync(keyword);
    }

    @FXML
    private void onReset() {
        if (busy) {
            return;
        }

        runSafely("초기화 실패", () -> {
            searchField.clear();
            clearForm(false);
            loadProductsAsync("전체 상품 목록으로 초기화되었습니다.");
        });
    }

    @FXML
    private void onCancelEdit() {
        if (busy) {
            return;
        }

        runSafely("수정 취소 실패", () -> {
            clearForm(true);
            showMessage("수정 모드가 취소되었습니다.", true);
        });
    }

    private void registerProductAsync(String productCode, int price) {
        setBusy(true);
        showMessage("상품 등록 중입니다...", true);

        Task<Product> task = new Task<>() {
            @Override
            protected Product call() {
                if (productRepository.existsByProductCode(productCode)) {
                    throw new IllegalArgumentException("이미 등록된 상품코드입니다.");
                }

                Product product = new Product(productCode, price);
                productRepository.save(product);

                return productRepository.findByProductCode(productCode);
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            Product savedProduct = task.getValue();

            if (savedProduct != null) {
                productList.add(0, savedProduct);
            } else {
                Product fallbackProduct = new Product(productCode, price);
                productList.add(0, fallbackProduct);
            }

            clearForm(true);
            showMessage("상품이 등록되었습니다. [상품코드: " + productCode + "]", true);
        });

        task.setOnFailed(event -> {
            setBusy(false);

            if (barcodeField != null) {
                barcodeField.requestFocus();
                barcodeField.selectAll();
            }

            handleTaskError("저장 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void updateProductAsync(String productCode, int price) {
        if (editingProduct == null) {
            showMessage("수정할 상품이 선택되지 않았습니다.", false);
            return;
        }

        if (!editingProduct.getProductCode().equalsIgnoreCase(productCode)) {
            barcodeField.setText(editingProduct.getProductCode());
            barcodeField.requestFocus();
            barcodeField.selectAll();
            showMessage("수정 시 상품코드는 변경할 수 없습니다.", false);
            return;
        }

        Long editingId = editingProduct.getId();

        setBusy(true);
        showMessage("상품 수정 중입니다...", true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                productRepository.update(editingId, price);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            for (Product item : productList) {
                if (item != null && item.getId() != null && item.getId().equals(editingId)) {
                    item.setPrice(price);
                    break;
                }
            }

            if (productTable != null) {
                productTable.refresh();
            }

            clearForm(true);
            showMessage("상품이 수정되었습니다. [ID: " + editingId + "]", true);
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("수정 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void deleteProduct(Product product) {
        if (busy) {
            return;
        }

        runSafely("상품 삭제 실패", () -> {
            if (product == null || product.getId() == null) {
                throw new IllegalArgumentException("삭제할 상품이 없습니다.");
            }

            boolean confirmed = showDeleteConfirmDialog(product);

            if (!confirmed) {
                return;
            }

            deleteProductAsync(product);
        });
    }

    private boolean showDeleteConfirmDialog(Product product) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("상품 삭제");

        Window owner = getOwnerWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setResizable(false);

        Label titleLabel = new Label("선택한 상품을 삭제하시겠습니까?");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label contentLabel = new Label(
                "ID: " + product.getId() + "\n상품코드: " + product.getProductCode()
        );

        VBox contentBox = new VBox(12, titleLabel, contentLabel);
        contentBox.setStyle("-fx-padding: 20;");

        dialog.getDialogPane().setContent(contentBox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("삭제");
        }

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setText("취소");
        }

        Optional<ButtonType> result = dialog.showAndWait();

        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private Window getOwnerWindow() {
        if (productTable != null && productTable.getScene() != null) {
            return productTable.getScene().getWindow();
        }

        if (barcodeField != null && barcodeField.getScene() != null) {
            return barcodeField.getScene().getWindow();
        }

        if (searchField != null && searchField.getScene() != null) {
            return searchField.getScene().getWindow();
        }

        return null;
    }

    private void deleteProductAsync(Product product) {
        Long deleteId = product.getId();

        setBusy(true);
        showMessage("상품 삭제 중입니다...", true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                productRepository.deleteById(deleteId);
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);

            if (editingProduct != null
                    && editingProduct.getId() != null
                    && editingProduct.getId().equals(deleteId)) {
                clearForm(false);
            }

            productList.removeIf(item ->
                    item != null
                            && item.getId() != null
                            && item.getId().equals(deleteId)
            );

            if (productTable != null) {
                productTable.getSelectionModel().clearSelection();
                productTable.refresh();
            }

            showMessage("상품이 삭제되었습니다. [ID: " + deleteId + "]", true);
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("상품 삭제 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void searchProductsAsync(String keyword) {
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

            if (searchField != null) {
                searchField.requestFocus();
            }
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("조회 실패", task.getException());

            if (searchField != null) {
                searchField.requestFocus();
            }
        });

        startDaemonTask(task);
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

        return normalized.substring(0, 10).toUpperCase();
    }

    private void loadProductsAsync(String successMessage) {
        if (busy) {
            return;
        }

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
        });

        task.setOnFailed(event -> {
            setBusy(false);
            handleTaskError("상품 목록 조회 실패", task.getException());
        });

        startDaemonTask(task);
    }

    private void clearForm(boolean requestBarcodeFocus) {
        editingProduct = null;

        if (barcodeField != null) {
            barcodeField.clear();
            barcodeField.setDisable(false);
        }

        if (priceField != null) {
            priceField.clear();
        }

        if (productTable != null) {
            productTable.getSelectionModel().clearSelection();
        }

        updateFormState();

        if (requestBarcodeFocus && barcodeField != null) {
            barcodeField.requestFocus();
        }
    }

    private void updateFormState() {
        boolean editMode = editingProduct != null;

        if (registerButton != null) {
            registerButton.setText(editMode ? "상품 수정" : "상품 등록");
        }

        if (cancelEditButton != null) {
            cancelEditButton.setVisible(editMode);
            cancelEditButton.setManaged(editMode);
        }
    }

    private void setBusy(boolean busy) {
        this.busy = busy;

        if (searchButton != null) {
            searchButton.setDisable(busy);
        }

        if (resetButton != null) {
            resetButton.setDisable(busy);
        }

        if (registerButton != null) {
            registerButton.setDisable(busy);
        }

        if (cancelEditButton != null) {
            cancelEditButton.setDisable(busy);
        }

        if (barcodeField != null) {
            barcodeField.setDisable(editingProduct != null);
        }

        updateFormState();

        if (registerButton != null) {
            registerButton.setDisable(busy);
        }

        if (cancelEditButton != null) {
            cancelEditButton.setDisable(busy);
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
            message = title + " - 처리 중 오류가 발생했습니다.\n" + error.getMessage();
        } else {
            message = title + " - 처리 중 알 수 없는 오류가 발생했습니다.";
        }

        showMessage(message, false);
    }

    private void runSafely(String errorTitle, Runnable action) {
        try {
            action.run();

        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), false);

        } catch (Exception e) {
            e.printStackTrace();
            showMessage(errorTitle + " - 처리 중 오류가 발생했습니다.\n" + e.getMessage(), false);
        }
    }

    private void showMessage(String message, boolean success) {
        if (messageLabel == null) {
            return;
        }

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