package com.dormhub.util;

import com.dormhub.config.PaymentQrProperties;
import com.dormhub.entity.Payment;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class VietQrUrlBuilder {

    private final PaymentQrProperties properties;

    public VietQrUrlBuilder(PaymentQrProperties properties) {
        this.properties = properties;
    }

    public String build(Payment payment) {
        return UriComponentsBuilder.fromUriString(
                        "https://img.vietqr.io/image/{bank}-{account}-{template}.svg")
                .queryParam("amount", payment.getExpectedAmount().setScale(0, RoundingMode.UNNECESSARY).toPlainString())
                .queryParam("addInfo", payment.getPaymentReference())
                .buildAndExpand(payment.getReceivingBankCode(), payment.getReceivingAccountNumber(), properties.getTemplate())
                .encode()
                .toUriString();
    }
}
