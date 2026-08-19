package com.l7pos.l7_pos.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * 표에 금액과 수량을 보기 좋게 표시하는 공통 서식.
 *
 * 금액은 자릿수가 커서 "116000" 처럼 붙여 쓰면 읽기 어렵다.
 * 천 단위 구분을 넣고 오른쪽으로 붙여 자릿수를 맞춘다.
 */
public class TableFormats {

    private TableFormats() {
    }

    /**
     * 금액 컬럼: 116000 -> "116,000원", 오른쪽 정렬
     */
    public static <S> void applyMoney(TableColumn<S, Number> column) {
        apply(column, "원");
    }

    /**
     * 수량 컬럼: 4 -> "4개", 오른쪽 정렬
     */
    public static <S> void applyCount(TableColumn<S, Number> column) {
        apply(column, "개");
    }

    private static <S> void apply(TableColumn<S, Number> column, String suffix) {
        if (column == null) {
            return;
        }

        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null) {
                    setText(null);
                    return;
                }

                setText(NumberFormat.getNumberInstance(Locale.KOREA).format(value) + suffix);
            }

            {
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });
    }
}
