package com.l7pos.l7_pos;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;

public class L7POSApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource("/com/l7pos/l7_pos/main-view.fxml");
        System.out.println("FXML 경로 확인: " + fxmlLocation);

        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(fxmlLocation, "main-view.fxml 파일을 찾을 수 없습니다.")
        );

        Parent root = loader.load();
        Scene scene = new Scene(root, 1000, 700);
        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/icon.png")
                        )
                )
        );
        stage.setTitle("L7 POS");
        stage.setScene(scene);
        stage.show();
    }
}