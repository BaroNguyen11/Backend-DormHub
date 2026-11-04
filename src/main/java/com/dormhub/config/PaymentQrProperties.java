package com.dormhub.config;

import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentQrProperties {

    private final String bankCode;
    private final String accountNumber;
    private final String referencePrefix;
    private final int referenceSuffixLength;
    private final String referenceCharacterType;
    private final int ttlMinutes;
    private final String template;

    public PaymentQrProperties(
            @Value("${dormhub.payment.test-bank-code:}") String bankCode,
            @Value("${dormhub.payment.test-account-number:}") String accountNumber,
            @Value("${dormhub.payment.reference-prefix:}") String referencePrefix,
            @Value("${dormhub.payment.reference-suffix-length:0}") int referenceSuffixLength,
            @Value("${dormhub.payment.reference-character-type:}") String referenceCharacterType,
            @Value("${dormhub.payment.ttl-minutes:15}") int ttlMinutes,
            @Value("${dormhub.payment.qr-template:compact}") String template) {
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.referencePrefix = referencePrefix;
        this.referenceSuffixLength = referenceSuffixLength;
        this.referenceCharacterType = referenceCharacterType;
        this.ttlMinutes = ttlMinutes;
        this.template = template;
    }

    public void validateForInitiation() {
        if (bankCode == null || !bankCode.matches("[A-Za-z0-9]{2,32}")
                || accountNumber == null || !accountNumber.matches("[A-Za-z0-9]{1,19}")
                || !"DH".equals(referencePrefix)
                || referenceSuffixLength != 12
                || !"ALPHANUMERIC".equals(referenceCharacterType)
                || ttlMinutes < 1 || ttlMinutes > 1440
                || !("compact".equals(template) || "compact2".equals(template)
                        || "qr_only".equals(template) || "qronly".equals(template)
                        || "print".equals(template) || "standee".equals(template))) {
            throw new AppException(ErrorCode.PAYMENT_CONFIGURATION_ERROR);
        }
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getReferencePrefix() {
        return referencePrefix;
    }

    public int getReferenceSuffixLength() {
        return referenceSuffixLength;
    }

    public String getReferenceCharacterType() {
        return referenceCharacterType;
    }

    public Duration getTtl() {
        return Duration.ofMinutes(ttlMinutes);
    }

    public String getTemplate() {
        String value = template.toLowerCase(Locale.ROOT);
        return "qronly".equals(value) ? "qr_only" : value;
    }
}
