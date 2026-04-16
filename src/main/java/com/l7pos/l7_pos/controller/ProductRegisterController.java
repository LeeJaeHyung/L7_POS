package com.l7pos.l7_pos.controller;

import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.repository.ProductRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProductRegisterController {

    @FXML
    private TextField barcodeField;

    @FXML
    private TextField priceField;

    @FXML
    private Button registerButton;

    @FXML
    private Button cancelEditButton;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<Product> productTable;

    @FXML
    private TableColumn<Product, Long> idColumn;

    @FXML
    private TableColumn<Product, String> barcodeColumn;

    @FXML
    private TableColumn<Product, Integer> priceColumn;

    @FXML
    private TableColumn<Product, Product> actionColumn;

    private final ObservableList<Product> productList = FXCollections.observableArrayList();
    private final ProductRepository productRepository = new ProductRepository();

    private Product editingProduct;

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        barcodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));

        priceColumn.setCellFactory(column -> new TableCell<>() {
            private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.KOREA);

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(numberFormat.format(item) + " 원");
                }
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

                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(buttonBox);
                }
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

        loadProducts();
        updateFormState();
        barcodeField.requestFocus();
    }

    @FXML
    private void onSave() {
        String barcode = barcodeField.getText() == null ? "" : barcodeField.getText().trim();
        String priceText = priceField.getText() == null ? "" : priceField.getText().trim();

        if (barcode.isEmpty()) {
            showMessage("바코드를 입력해주세요.", false);
            barcodeField.requestFocus();
            return;
        }

        if (priceText.isEmpty()) {
            showMessage("가격을 입력해주세요.", false);
            priceField.requestFocus();
            return;
        }

        int price;
        try {
            price = Integer.parseInt(priceText);
        } catch (NumberFormatException e) {
            showMessage("가격은 숫자만 입력 가능합니다.", false);
            priceField.requestFocus();
            priceField.selectAll();
            return;
        }

        if (price <= 0) {
            showMessage("가격은 0보다 커야 합니다.", false);
            priceField.requestFocus();
            priceField.selectAll();
            return;
        }

        try {
            String productCode = extractProductCode(barcode);

            if (editingProduct == null) {
                registerProduct(productCode, price);
            } else {
                updateProduct(productCode, price);
            }

        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage(), false);
            barcodeField.requestFocus();
            barcodeField.selectAll();
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("처리 중 오류가 발생했습니다.", false);
        }
    }

    @FXML
    private void onCancelEdit() {
        clearForm();
        showMessage("수정 모드가 취소되었습니다.", true);
    }

    private void registerProduct(String productCode, int price) {
        if (productRepository.existsByProductCode(productCode)) {
            showMessage("이미 등록된 상품코드입니다.", false);
            barcodeField.requestFocus();
            barcodeField.selectAll();
            return;
        }

        Product product = new Product(productCode, price);
        productRepository.save(product);

        loadProducts();
        clearForm();
        showMessage("상품이 등록되었습니다. [상품코드: " + productCode + "]", true);
    }

    private void updateProduct(String productCode, int price) {
        if (editingProduct == null) {
            showMessage("수정할 상품이 선택되지 않았습니다.", false);
            return;
        }

        if (!editingProduct.getProductCode().equals(productCode)) {
            showMessage("수정 시 상품코드는 변경할 수 없습니다.", false);
            barcodeField.setText(editingProduct.getProductCode());
            barcodeField.requestFocus();
            barcodeField.selectAll();
            return;
        }

        Long editingId = editingProduct.getId();
        productRepository.update(editingId, price);

        loadProducts();
        clearForm();
        showMessage("상품이 수정되었습니다. [ID: " + editingId + "]", true);
    }

    private void startEditProduct(Product product) {
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
    }

    private void deleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("상품 삭제");
        alert.setHeaderText("선택한 상품을 삭제하시겠습니까?");
        alert.setContentText("ID: " + product.getId() + " / 상품코드: " + product.getProductCode());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            Long deleteId = product.getId();
            productRepository.deleteById(deleteId);
            loadProducts();

            if (editingProduct != null && editingProduct.getId().equals(deleteId)) {
                clearForm();
            }

            showMessage("상품이 삭제되었습니다. [ID: " + deleteId + "]", true);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("상품 삭제 중 오류가 발생했습니다.", false);
        }
    }

    /**
     * 전체 바코드에서 컬러/사이즈를 제외한 상품 기준 코드 추출
     * 예: co2302st17bks -> co2302st17
     */
    private String extractProductCode(String barcode) {
        if (barcode == null) {
            throw new IllegalArgumentException("바코드를 입력해주세요.");
        }

        String normalized = barcode.trim().toLowerCase();

        if (normalized.length() < 10) {
            throw new IllegalArgumentException("바코드 형식이 올바르지 않습니다.");
        }

        return normalized.substring(0, 10);
    }

    private void loadProducts() {
        try {
            List<Product> products = productRepository.findAll();
            productList.setAll(products);
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("상품 목록 조회 중 오류가 발생했습니다.", false);
        }
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

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");

        if (success) {
            messageLabel.getStyleClass().add("message-success");
        } else {
            messageLabel.getStyleClass().add("message-error");
        }

        updateFormState();
    }
}