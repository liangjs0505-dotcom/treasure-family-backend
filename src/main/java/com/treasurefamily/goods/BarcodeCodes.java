package com.treasurefamily.goods;

import com.treasurefamily.common.BusinessException;
import java.security.SecureRandom;

/** EAN-13 校验。以 2 开头的是店内码或称重码，不进公用名录。 */
public final class BarcodeCodes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private BarcodeCodes() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        return digits.toString();
    }

    public static String requireRetailOrLabel(String raw) {
        String code = normalize(raw);
        if (code.length() != 13 || checkDigit(code.substring(0, 12)) != code.charAt(12)) {
            throw new BusinessException("条码不对，请再扫一次");
        }
        return code;
    }

    public static boolean internal(String code) {
        return code != null && !code.isEmpty() && code.charAt(0) == '2';
    }

    public static String nextInternal() {
        StringBuilder body = new StringBuilder("20");
        for (int i = 0; i < 10; i++) {
            body.append(RANDOM.nextInt(10));
        }
        body.append(checkDigit(body.toString()));
        return body.toString();
    }

    static char checkDigit(String twelve) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = twelve.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (char) ('0' + (10 - (sum % 10)) % 10);
    }
}
