package com.l7pos.l7_pos.util;

/**
 * 한글 입력 → 영문 자판 변환
 *
 * 한/영 전환을 깜빡하고 품번을 치면
 * "co2602st19" 대신 "채2602ㄴㅅ19" 처럼 입력된다.
 * 두벌식 자판 기준으로 원래 눌렀던 키를 되돌려준다.
 *
 * 한글이 아닌 글자(영문, 숫자, 기호)는 그대로 통과시키므로
 * 이미 영문으로 친 입력에는 아무 영향이 없다.
 */
public class HangulKeyboardConverter {

    private static final char HANGUL_SYLLABLE_BASE = 0xAC00;   // 가
    private static final char HANGUL_SYLLABLE_LAST = 0xD7A3;   // 힣

    private static final char COMPAT_JAMO_BASE = 0x3131;       // ㄱ
    private static final char COMPAT_JAMO_LAST = 0x3163;       // ㅣ

    private static final int MEDIAL_COUNT = 21;
    private static final int FINAL_COUNT = 28;

    /** 초성 19개의 두벌식 자판 키 */
    private static final String[] INITIALS = {
            "r", "R", "s", "e", "E", "f", "a", "q", "Q", "t",
            "T", "d", "w", "W", "c", "z", "x", "v", "g"
    };

    /** 중성 21개의 두벌식 자판 키 (복합 모음은 두 키) */
    private static final String[] MEDIALS = {
            "k", "o", "i", "O", "j", "p", "u", "P", "h", "hk",
            "ho", "hl", "y", "n", "nj", "np", "nl", "b", "m", "ml",
            "l"
    };

    /** 종성 28개의 두벌식 자판 키 (0번은 받침 없음, 겹받침은 두 키) */
    private static final String[] FINALS = {
            "", "r", "R", "rt", "s", "sw", "sg", "e", "f", "fr",
            "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "qt", "t",
            "T", "d", "w", "c", "z", "x", "v", "g"
    };

    /**
     * 낱자로 남은 자모(ㄱ~ㅣ, U+3131~U+3163)의 두벌식 자판 키
     *
     * 모음 없이 자음만 연달아 치면 조합되지 않고
     * 이 영역의 글자로 남는다. ("st" -> "ㄴㅅ")
     */
    private static final String[] COMPAT_JAMOS = {
            // 자음 ㄱ ~ ㅎ (U+3131 ~ U+314E)
            "r", "R", "rt", "s", "sw", "sg", "e", "E", "f", "fr",
            "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "Q", "qt",
            "t", "T", "d", "w", "W", "c", "z", "x", "v", "g",
            // 모음 ㅏ ~ ㅣ (U+314F ~ U+3163)
            "k", "o", "i", "O", "j", "p", "u", "P", "h", "hk",
            "ho", "hl", "y", "n", "nj", "np", "nl", "b", "m", "ml",
            "l"
    };

    private HangulKeyboardConverter() {
    }

    /**
     * 한글로 입력된 부분만 영문 자판 키로 되돌린다.
     *
     * 예) "채2602ㄴㅅ19" -> "co2602st19"
     */
    public static String toEnglish(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        StringBuilder result = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (ch >= HANGUL_SYLLABLE_BASE && ch <= HANGUL_SYLLABLE_LAST) {
                appendSyllable(result, ch);

            } else if (ch >= COMPAT_JAMO_BASE && ch <= COMPAT_JAMO_LAST) {
                result.append(COMPAT_JAMOS[ch - COMPAT_JAMO_BASE]);

            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    /**
     * 입력에 한글이 섞여 있는지 확인한다.
     * 안내 메시지를 띄울지 판단할 때 쓴다.
     */
    public static boolean containsHangul(String value) {
        if (value == null) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            boolean isSyllable = ch >= HANGUL_SYLLABLE_BASE && ch <= HANGUL_SYLLABLE_LAST;
            boolean isJamo = ch >= COMPAT_JAMO_BASE && ch <= COMPAT_JAMO_LAST;

            if (isSyllable || isJamo) {
                return true;
            }
        }

        return false;
    }

    /**
     * 완성형 한 글자를 초성 / 중성 / 종성으로 쪼개 각각의 자판 키를 붙인다.
     */
    private static void appendSyllable(StringBuilder result, char syllable) {
        int offset = syllable - HANGUL_SYLLABLE_BASE;

        int initialIndex = offset / (MEDIAL_COUNT * FINAL_COUNT);
        int medialIndex = (offset % (MEDIAL_COUNT * FINAL_COUNT)) / FINAL_COUNT;
        int finalIndex = offset % FINAL_COUNT;

        result.append(INITIALS[initialIndex])
                .append(MEDIALS[medialIndex])
                .append(FINALS[finalIndex]);
    }
}
