package com.l7pos.l7_pos.dto;

import com.l7pos.l7_pos.entity.Product;
import com.l7pos.l7_pos.util.ParsedBarcode;

/**
 * 바코드 확정 결과
 *
 * recovered 가 true 면 입력 앞에 붙은 잘못된 글자를 떼어내고 찾은 것이다.
 * 이 경우 화면에 알려줘서, 잘못 인식된 것은 아닌지 확인할 수 있게 한다.
 */
public record ResolvedBarcode(ParsedBarcode parsed,
                              Product product,
                              String rawInput,
                              boolean recovered) {
}
