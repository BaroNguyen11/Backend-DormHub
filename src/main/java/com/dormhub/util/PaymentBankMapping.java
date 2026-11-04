package com.dormhub.util;

public final class PaymentBankMapping {

    private static final String ACB_BIN = "970416";
    private static final String MB_BIN = "970422";

    private PaymentBankMapping() {
    }

    public static String sepayGatewayBin(String gateway) {
        if ("ACB".equals(gateway)) {
            return ACB_BIN;
        }
        return "MBBank".equals(gateway) ? MB_BIN : null;
    }

    public static String vietQrBankBin(String bankCode) {
        if (ACB_BIN.equals(bankCode) || "ACB".equals(bankCode)) {
            return ACB_BIN;
        }
        return MB_BIN.equals(bankCode) || "MB".equals(bankCode) ? MB_BIN : null;
    }
}
