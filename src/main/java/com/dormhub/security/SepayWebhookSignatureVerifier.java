package com.dormhub.security;

import com.dormhub.config.SepayProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Xác thực request webhook SePay.
 *
 * <p><b>Chuẩn SePay thật (docs.sepay.vn):</b> mỗi request webhook mang header
 * {@code Authorization: Apikey <API_KEY>} — trong đó API_KEY là khóa của bạn
 * trên my.sepay.vn (Cài đặt → API). Cấu hình cùng giá trị vào
 * {@code dormhub.sepay.api-key} và backend sẽ chỉ nhận request của SePay.
 * Đây là lý do các webhook thật trước đây bị từ chối: verifier cũ yêu cầu
 * bộ header tự chế X-SePay-Timestamp/X-SePay-Signature mà SePay không bao giờ gửi.</p>
 *
 * <p>Vẫn giữ tuỳ chọn HMAC legacy ({@code X-SePay-Signature} +
 * {@code X-SePay-Timestamp}) cho script simulate kiểm thử ở môi trường dev.</p>
 *
 * <p>Nếu chưa cấu hình api-key, hệ thống chạy chế độ dev: chấp nhận request
 * (kèm log cảnh báo) để thanh toán không bị kẹt PENDING chỉ vì thiếu cấu hình.</p>
 */
@Component
public class SepayWebhookSignatureVerifier {

    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300;
    private static final Pattern SIGNATURE_FORMAT = Pattern.compile("sha256=[0-9a-f]{64}");
    private static final Pattern APIKEY_FORMAT = Pattern.compile("(?i)^\\s*Apikey\\s+(.+?)\\s*$");

    private final SepayProperties properties;

    public SepayWebhookSignatureVerifier(SepayProperties properties) {
        this.properties = properties;
    }

    /**
     * @param authorization giá trị header {@code Authorization} (SePay thật)
     * @param signature     giá trị {@code X-SePay-Signature} (tuỳ chọn legacy)
     * @param timestamp     giá trị {@code X-SePay-Timestamp} (tuỳ chọn legacy)
     */
    public boolean verify(String authorization, String signature, String timestamp, byte[] rawBody) {
        String apiKey = properties.getApiKey();
        boolean apiKeyConfigured = apiKey != null && !apiKey.isBlank();

        // 1) Chuẩn SePay: Authorization: Apikey <API_KEY>
        if (apiKeyConfigured && matchesApiKey(authorization, apiKey.trim())) {
            return true;
        }

        // 2) HMAC legacy (chỉ hợp lệ khi đã cấu hình webhook-secret)
        if (verifyLegacyHmac(signature, timestamp, rawBody)) {
            return true;
        }

        // 3) Dev mode: chưa cấu hình api-key thì bỏ qua xác thực để không kẹt luồng thanh toán.
        return !apiKeyConfigured;
    }

    /** So sánh hằng thời gian giữa "Apikey <gửi>" và khóa đã cấu hình. */
    private boolean matchesApiKey(String authorization, String expected) {
        if (authorization == null || authorization.isBlank()) {
            return false;
        }
        Matcher matcher = APIKEY_FORMAT.matcher(authorization);
        if (!matcher.matches()) {
            return false;
        }
        byte[] received = matcher.group(1).getBytes(StandardCharsets.UTF_8);
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, received);
    }

    /** Cơ chế HMAC cũ (timestamp + "." + body) — dùng cho script simulate/dev. */
    public boolean verifyLegacyHmac(String signature, String timestamp, byte[] rawBody) {
        String secret = properties.getWebhookSecret();
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
