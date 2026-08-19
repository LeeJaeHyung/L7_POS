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

        /*
         * 창 크기
         *
         * 상품 관리 화면이 1150px 가 필요하고 좌측 메뉴가 184px 이라
         * 기존 1000px 로는 내용이 잘렸다.
         */
        Scene scene = new Scene(root, 1440, 880);

        /*
         * 공통 스타일시트
         *
         * Scene 에 한 번만 붙이면 나중에 갈아 끼우는 화면들에도 그대로 적용된다.
         */
        URL stylesheet = getClass().getResource("/com/l7pos/l7_pos/css/app.css");

        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        } else {
            System.err.println("스타일시트를 찾을 수 없습니다: /com/l7pos/l7_pos/css/app.css");
        }

        stage.getIcons().add(
                new javafx.scene.image.Image(
                        Objects.requireNonNull(
                                getClass().getResourceAsStream("/icon.png")
                        )
                )
        );
        stage.setTitle("L7 POS");
        stage.setScene(scene);
        stage.setMinWidth(1180);
        stage.setMinHeight(720);
        stage.show();
    }
}