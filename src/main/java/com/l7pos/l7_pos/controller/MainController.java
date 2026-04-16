package com.l7pos.l7_pos.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void goSale() {
        loadPage("/com/l7pos/l7_pos/sale-view.fxml");
    }

    @FXML
    public void goSalesHistory() {
        loadPage("/com/l7pos/l7_pos/sales-history-view.fxml");
    }

    @FXML
    public void goPriceSetting() {
        loadPage("/com/l7pos/l7_pos/ProductRegisterView.fxml");
    }

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}