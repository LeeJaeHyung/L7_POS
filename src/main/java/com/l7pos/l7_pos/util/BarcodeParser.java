package com.l7pos.l7_pos.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarcodeParser {

    public static final int PRODUCT_CODE_LENGTH = 10;

    private static final int COLOR_CODE_LENGTH = 2;

    /**
     * 품번 접두사.
     *
     * 모든 품번이 이 글자로 시작한다는 점을 이용해
     * 앞에 잘못 붙은 글자를 떼어낸다.
     */
    private static final String PRODUCT_CODE_PREFIX = "CO";

    private static final int MINIMUM_BARCODE_LENGTH =
            PRODUCT_CODE_LENGTH + COLOR_CODE_LENGTH + 1;

    /**
     * 검색용 품번 추출
     *
     * 바코드 = 품번(10자리) + 컬러(2자리) + 사이즈 구조이므로
     * 앞 10자리만 잘라내면 컬러/사이즈와 무관한 품번이 된다.
     * 바코드를 통째로 입력해도, 품번만 입력해도 같은 결과가 나온다.
     *
     * 한글로 입력된 경우("채2602ㄴㅅ19")도 영문 자판으로 되돌려 처리한다.
     */
    public static String toProductCode(String value) {
        if (value == null) {
            return "";
        }

        // 한/영 전환을 안 하고 친 경우를 대비해 한글을 영문 자판으로 되돌린다.
        String normalized = HangulKeyboardConverter.toEnglish(value)
                .trim()
                .toUpperCase(Locale.ROOT);

        // 앞에 잘못 눌린 글자가 붙었으면 품번이 시작하는 곳부터 자른다.
        normalized = stripLeadingNoise(normalized);

        return normalized.length() > PRODUCT_CODE_LENGTH
                ? normalized.substring(0, PRODUCT_CODE_LENGTH)
                : normalized;
    }

    /**
     * 앞에 붙은 잘못된 글자를 떼어낸다.
     *
     * 스캔하기 전에 키보드 위에 물건이 닿아
     * "." 이나 "ㅐ" 같은 글자가 먼저 입력되는 경우가 있다.
     *
     * 품번은 CO 로 시작하므로 CO 가 나오는 곳부터 남기되,
     * 잡글자 안에도 CO 가 섞일 수 있으므로 "마지막" CO 를 기준으로 한다.
     * 예) "XCOCO2602ST19BKS" -> "CO2602ST19BKS"
     *
     * CO 가 없으면 원래 값을 그대로 돌려준다.
     */
    private static String stripLeadingNoise(String value) {
        if (value.startsWith(PRODUCT_CODE_PREFIX)) {
            return value;
        }

        int index = value.lastIndexOf(PRODUCT_CODE_PREFIX);

        return index < 0 ? value : value.substring(index);
    }

    /**
     * 잘못된 글자가 섞인 입력에서 실제 바코드 후보들을 뽑는다.
     *
     * 잡글자 안에도 CO 가 우연히 들어갈 수 있으므로
     * "마지막" CO 부터 앞쪽으로 거슬러 올라가며 후보를 만든다.
     * 즉 가장 그럴듯한 후보가 맨 앞에 온다.
     *
     * 어느 후보가 진짜인지는 상품이 등록돼 있는지로 최종 판단한다.
     */
    public static List<String> recoverBarcodeCandidates(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        String normalized = HangulKeyboardConverter.toEnglish(value)
                .trim()
                .toUpperCase(Locale.ROOT);

        List<String> candidates = new ArrayList<>();

        int index = normalized.lastIndexOf(PRODUCT_CODE_PREFIX);

        while (index >= 0) {
            String candidate = normalized.substring(index);

            if (candidate.length() >= MINIMUM_BARCODE_LENGTH) {
                candidates.add(candidate);
            }

            index = index == 0
                    ? -1
                    : normalized.lastIndexOf(PRODUCT_CODE_PREFIX, index - 1);
        }

        return candidates;
    }

    public static ParsedBarcode parse(String barcode) {

        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("바코드를 입력하세요.");
        }

        // 🔥 핵심: 대소문자 무시 처리
        barcode = barcode.trim().toUpperCase(Locale.ROOT);

        if (barcode.length() < MINIMUM_BARCODE_LENGTH) {
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