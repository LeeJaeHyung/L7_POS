package com.l7pos.l7_pos.util;

import java.util.Locale;

public class BarcodeParser {

    private static final int PRODUCT_CODE_LENGTH = 10;
    private static final int COLOR_CODE_LENGTH = 2;

    public static ParsedBarcode parse(String barcode) {

        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("바코드를 입력하세요.");
        }

        // 🔥 핵심: 대소문자 무시 처리
        barcode = barcode.trim().toUpperCase(Locale.ROOT);

        int minimumLength = PRODUCT_CODE_LENGTH + COLOR_CODE_LENGTH + 1;

        if (barcode.length() < minimumLength) {
            throw new IllegalArgumentException("바코드 형식이 올바르지 않습니다: " + barcode);
        }

        String productCode = barcode.substring(0, PRODUCT_CODE_LENGTH).toUpperCase(Locale.ROOT);
        String color = barcode.substring(PRODUCT_CODE_LENGTH, PRODUCT_CODE_LENGTH + COLOR_CODE_LENGTH).toUpperCase(Locale.ROOT);
        String size = barcode.substring(PRODUCT_CODE_LENGTH + COLOR_CODE_LENGTH).toUpperCase(Locale.ROOT);

        if (size.isBlank()) {
            throw new IllegalArgumentException("사이즈 코드가 없습니다: " + barcode);
        }

        return new ParsedBarcode(
                barcode,
                productCode,
                color,
                size
        );
    }
}