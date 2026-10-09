package com.treasurefamily.goods;

import com.treasurefamily.common.BusinessException;
import java.security.SecureRandom;

/** 包装条码最长 16 位，不校验。以 2 开头的是店内码或称重码，不进公用名录。 */
public final class BarcodeCodes {

    private static final SecureRandom RANDOM = new SecureRandom();
    static final int MAX_LENGTH = 16;

    private BarcodeCodes() {
    }

    public static String requireRetailOrLabel(String raw) {
        String code = raw == null ? "" : raw.trim();
        if (code.isEmpty()) {
            throw new BusinessException("条码不能为空");
        }
        if (code.length() > MAX_LENGTH) {
            throw new BusinessException("条码最多 16 位");
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
