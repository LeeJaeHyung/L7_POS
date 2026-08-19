package com.l7pos.l7_pos.util;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;


/**
 * 품번/바코드 입력창을 항상 영문 대문자로 받게 만든다.
 *
 * 왜 필요한가:
 * macOS + JavaFX 조합에서 한글 IME 를 켜고 입력하면
 * 조합 중이던 글자가 필드에 도착하기 전에 사라진다.
 *
 *   "co2602st13" 입력 -> 필드에는 "ㅊ2602ㄴ13"
 *   ('o'가 만든 ㅐ, 't'가 만든 ㅅ 이 유실)
 *
 * 이미 글자가 없어진 뒤라서
 * 나중에 문자열을 변환하는 방법으로는 되살릴 수 없다.
 *
 * 그래서 두 단계로 막는다.
 *
 *  1) 이 필드에서는 OS 입력기(IME) 자체를 끈다.
 *     그러면 한글 조합이 일어나지 않고 영문 키가 그대로 들어온다.
 *
 *  2) 그래도 IME 가 동작하는 환경이면,
 *     실제로 눌린 키(KeyCode)를 읽어 영문자를 직접 넣는다.
 *
 * 중요:
 * 2번이 통하지 않는 환경(키코드가 UNDEFINED 로 오는 경우)에서는
 * 아무것도 막지 않고 원래 입력을 그대로 통과시킨다.
 * 잘못 막아서 입력이 아예 안 되는 상황을 만들지 않기 위해서다.
 *
 * 진단이 필요하면 -Dl7pos.debugInput=true 로 실행하면
 * 어떤 입력 이벤트가 들어오는지 콘솔에 찍힌다.
 */
public class EnglishInputGuard {

    private static final boolean DEBUG =
            Boolean.getBoolean("l7pos.debugInput");

    private EnglishInputGuard() {
    }

    public static void install(TextField field) {
        if (field == null) {
            return;
        }

        // 1. 이 필드에서는 OS 입력기를 끈다.
        disableInputMethod(field);

        /*
         * 직전 KEY_PRESSED 에서 우리가 글자를 직접 넣었는지 기억한다.
         *
         * 넣었을 때만 뒤따라오는 KEY_TYPED / IME 텍스트를 막는다.
         * 못 넣었으면 아무것도 막지 않는다. (입력이 죽지 않게)
         */
        boolean[] insertedFromKey = {false};

        // 2. 실제로 눌린 키를 읽어 영문 대문자로 직접 넣는다.
        field.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            debug("KEY_PRESSED", "code=" + event.getCode()
                    + " text=\"" + event.getText() + "\"");

            // 단축키(Cmd+A, Cmd+V, Ctrl+C 등)는 건드리지 않는다.
            if (event.isShortcutDown() || event.isAltDown()) {
                insertedFromKey[0] = false;
                return;
            }

            String typed = toInputCharacter(event.getCode());

            // 엔터, 백스페이스, 방향키 등은 원래 동작에 맡긴다.
            if (typed == null) {
                insertedFromKey[0] = false;
                return;
            }

            insertedFromKey[0] = true;

            event.consume();
            field.replaceSelection(typed);
        });

        /*
         * 3. 같은 글자가 KEY_TYPED 로 또 들어오는 것을 막는다.
         *    2번에서 넣지 못했다면 그대로 통과시킨다.
         */
        field.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            debug("KEY_TYPED", "char=\"" + event.getCharacter() + "\""
                    + " 우리가넣음=" + insertedFromKey[0]);

            if (insertedFromKey[0]) {
                insertedFromKey[0] = false;
                event.consume();
            }
        });

        /*
         * 4. IME 가 만든 한글이 필드에 들어오는 것을 막는다.
         *    역시 2번에서 넣었을 때만 막는다.
         */
        field.addEventFilter(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, event -> {
            debug("INPUT_METHOD", "committed=\"" + event.getCommitted() + "\""
                    + " 우리가넣음=" + insertedFromKey[0]);

            if (insertedFromKey[0]) {
                event.consume();
            }
        });
    }

    /**
     * 이 필드에서 OS 입력기를 끈다.
     *
     * JavaFX 는 포커스를 받은 노드에 입력기 요청(InputMethodRequests)이
     * 있을 때만 OS 입력기를 켠다.
     * 그래서 그것을 비워두면 한글 조합 자체가 일어나지 않는다.
     *
     * 스킨(TextFieldSkin)이 만들어질 때 다시 채워 넣으므로
     * 스킨이 준비된 뒤에 지워야 한다.
     */
    private static void disableInputMethod(TextField field) {
        Runnable clear = () -> {
            field.setInputMethodRequests(null);
            field.setOnInputMethodTextChanged(null);
        };

        clear.run();

        field.skinProperty().addListener((observable, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(clear);
            }
        });

        // 스킨이 이미 붙어 있는 경우까지 확실히 처리한다.
        Platform.runLater(clear);
    }

    /**
     * 눌린 키를 품번에 쓸 글자로 바꾼다.
     *
     * 영문/숫자가 아니면 null 을 돌려주고 기본 동작에 맡긴다.
     * KeyCode 는 한/영 상태와 무관한 물리 키를 알려주므로
     * 한글 IME 가 켜져 있어도 'c' 키는 KeyCode.C 로 들어온다.
     */
    private static String toInputCharacter(KeyCode code) {
        if (code == null) {
            return null;
        }

        /*
         * enum 상수 이름을 쓴다.
         *
         * 영문자 키는 "A" ~ "Z",
         * 숫자 키는 "DIGIT0" ~ "DIGIT9" / "NUMPAD0" ~ "NUMPAD9" 로
         * 자바 언어 명세상 고정이라 OS 나 언어 설정에 영향받지 않는다.
         * (표시용 getName() 과 달리 바뀔 여지가 없다)
         */
        String name = code.name();

        if (code.isLetterKey() && name.length() == 1) {
            return name;
        }

        if (code.isDigitKey()) {
            if (name.startsWith("DIGIT")) {
                return name.substring("DIGIT".length());
            }

            if (name.startsWith("NUMPAD")) {
                return name.substring("NUMPAD".length());
            }
        }

        return null;
    }

    private static void debug(String event, String detail) {
        if (DEBUG) {
            System.out.println("[입력진단] " + event + " " + detail);
        }
    }
}
