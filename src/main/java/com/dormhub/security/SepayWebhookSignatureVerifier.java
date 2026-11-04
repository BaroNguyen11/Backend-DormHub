package com.dormhub.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SepayWebhookSignatureVerifier {

    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300;
    private static final Pattern SIGNATURE_FORMAT = Pattern.compile("sha256=[0-9a-f]{64}");

    private final String secret;

    public SepayWebhookSignatureVerifier(@Value("${dormhub.sepay.webhook-secret:}") String secret) {
        this.secret = secret;
    }

    public boolean verify(String signature, String timestamp, byte[] rawBody) {
        if (secret == null || secret.isBlank() || signature == null || timestamp == null
                || rawBody == null || !SIGNATURE_FORMAT.matcher(signature).matches()) {
            return false;
        }

        final long signedAt;
        try {
            signedAt = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            return false;
        }

        long now = Instant.now().getEpochSecond();
        if (signedAt < now - MAX_TIMESTAMP_SKEW_SECONDS
                || signedAt > now + MAX_TIMESTAMP_SKEW_SECONDS) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(timestamp.getBytes(StandardCharsets.US_ASCII));
            mac.update((byte) '.');
            byte[] expected = mac.doFinal(rawBody);
            byte[] received = HexFormat.of().parseHex(signature.substring("sha256=".length()));
            return MessageDigest.isEqual(expected, received);
        } catch (Exception exception) {
            return false;
        }
    }
}
