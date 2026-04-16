package com.l7pos.l7_pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class L7POSApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
                L7POSApplication.class.getResource("ProductRegisterView.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 1000, 650);
        scene.getStylesheets().add(
                L7POSApplication.class.getResource("/com/l7pos/l7_pos/css/product-register.css").toExternalForm()
        );

        stage.setTitle("L7 POS - 상품 등록");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
    }

}
