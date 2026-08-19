package com.l7pos.l7_pos.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MainController {

    @FXML
    private StackPane contentArea;

    @FXML
    private Button saleNavButton;

    @FXML
    private Button salesHistoryNavButton;

    @FXML
    private Button productNavButton;

    private String currentPagePath;
    private boolean loading = false;

    /** 현재 보고 있는 메뉴를 표시할 때 쓰는 스타일 */
    private static final String ACTIVE_NAV_STYLE_CLASS = "nav-button-active";

    @FXML
    public void initialize() {
        if (contentArea == null) {
            showErrorDialog(
                    "초기화 오류",
                    "contentArea가 연결되지 않았습니다.\nMainView.fxml의 fx:id를 확인하세요."
            );
        }
    }

    @FXML
    public void goSale() {
        loadPageSafely("/com/l7pos/l7_pos/sale-view.fxml", "판매 페이지");
        markActiveNav(saleNavButton);
    }

    @FXML
    public void goSalesHistory() {
        loadPageSafely("/com/l7pos/l7_pos/sales-history-view.fxml", "판매 조회 페이지");
        markActiveNav(salesHistoryNavButton);
    }

    @FXML
    public void goPriceSetting() {
        loadPageSafely("/com/l7pos/l7_pos/ProductRegisterView.fxml", "가격 설정 페이지");
        markActiveNav(productNavButton);
    }

    /**
     * 지금 보고 있는 메뉴만 파랗게 칠한다.
     */
    private void markActiveNav(Button activeButton) {
        for (Button navButton : new Button[]{saleNavButton, salesHistoryNavButton, productNavButton}) {
            if (navButton == null) {
                continue;
            }

            navButton.getStyleClass().remove(ACTIVE_NAV_STYLE_CLASS);

            if (navButton == activeButton) {
                navButton.getStyleClass().add(ACTIVE_NAV_STYLE_CLASS);
            }
        }
    }

    private void loadPageSafely(String fxmlPath, String pageName) {
        if (loading) {
            return;
        }

        if (contentArea == null) {
            showErrorDialog("화면 오류", "contentArea가 null입니다.");
            return;
        }

        if (fxmlPath == null || fxmlPath.isBlank()) {
            showErrorDialog("페이지 이동 오류", "FXML 경로가 비어 있습니다.");
            return;
        }

        if (Objects.equals(currentPagePath, fxmlPath)) {
            return;
        }

        loading = true;

        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                throw new IllegalArgumentException("FXML 파일을 찾을 수 없습니다.\n경로: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node page = loader.load();

            if (page == null) {
                throw new IllegalStateException("FXML 로딩 결과가 비어 있습니다.");
            }

            contentArea.getChildren().setAll(page);
            currentPagePath = fxmlPath;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showErrorPage(pageName, e.getMessage());
            showErrorDialog(pageName + " 이동 실패", e.getMessage());

        } catch (IOException e) {
            e.printStackTrace();
            String message = "FXML 파일을 읽는 중 오류가 발생했습니다.\n\n" + getSafeMessage(e);
            showErrorPage(pageName, message);
            showErrorDialog(pageName + " 이동 실패", message);

        } catch (RuntimeException e) {
            e.printStackTrace();

            String message = """
                    화면을 불러오는 중 오류가 발생했습니다.

                    가능한 원인:
                    - FXML의 fx:controller 경로 오류
                    - FXML의 fx:id와 Controller 필드명 불일치
                    - Controller initialize() 내부 오류
                    - DB 연결 실패
                    - Service 또는 Repository 생성 실패
                    - CSS, 이미지 등 리소스 경로 오류

                    오류 내용:
                    """ + getSafeMessage(e);

            showErrorPage(pageName, message);
            showErrorDialog(pageName + " 이동 실패", message);

        } catch (Exception e) {
            e.printStackTrace();
            String message = "알 수 없는 오류가 발생했습니다.\n\n" + getSafeMessage(e);
            showErrorPage(pageName, message);
            showErrorDialog(pageName + " 이동 실패", message);

        } finally {
            loading = false;
        }
    }

    private void showErrorPage(String pageName, String message) {
        Label errorLabel = new Label(
                "[" + nullToDefault(pageName, "페이지") + "]를 불러오지 못했습니다.\n\n"
                        + nullToDefault(message, "오류 내용이 없습니다.")
        );

        errorLabel.setWrapText(true);
        errorLabel.setStyle("""
                -fx-padding: 30;
                -fx-font-size: 15px;
                -fx-text-fill: #c0392b;
                -fx-background-color: #fff5f5;
                -fx-border-color: #e74c3c;
                -fx-border-width: 1;
                -fx-border-radius: 8;
                -fx-background-radius: 8;
                """);

        if (contentArea != null) {
            contentArea.getChildren().setAll(errorLabel);
        }
    }

    private void showErrorDialog(String title, String message) {
        try {
            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    nullToDefault(message, "오류가 발생했습니다."),
                    ButtonType.OK
            );
            alert.setTitle(nullToDefault(title, "오류"));
            alert.setHeaderText(null);
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSafeMessage(Throwable e) {
        if (e == null) {
            return "알 수 없는 오류";
        }

        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }

        Throwable cause = e.getCause();

        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }

        return e.getClass().getSimpleName();
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}