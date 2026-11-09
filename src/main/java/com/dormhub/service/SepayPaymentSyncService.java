package com.dormhub.service;

import com.dormhub.config.SepayProperties;
import com.dormhub.dto.request.SepayWebhookPayload;
import com.dormhub.dto.response.PaymentSyncResponse;
import com.dormhub.service.SepayWebhookService.Outcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Đối soát CHỦ ĐỘNG (pull) giao dịch SePay — cơ chế dự phòng cho webhook.
 *
 * <p>Webhook là cơ chế chính: SePay đẩy giao dịch tới
 * {@code POST /api/v1/payments/webhooks/sepay} và {@link SepayWebhookService}
 * tự ghép vào phiên thanh toán. Nhưng nếu webhook lỡ/bị lỗi mạng, sinh viên
 * đã chuyển khoản xong vẫn sẽ kẹt PENDING. Service này xử lý bù bằng cách
 * gọi SePay Transactions API ({@code my.sepay.vn/userapi/transactions/list})
 * kéo các giao dịch gần nhất về và đưa vào cùng một đường {@code reconcile()}
 * với webhook — nên mọi quy tắc khớp tiền/mã tham chiếu/tài khoản/expiration
 * được dùng lại nguyên vẹn, kết quả pending → success (với hóa đơn cọc là
 * hợp đồng ACTIVE + tự phân phòng).</p>
 *
 * <p>Được gọi tại 2 thời điểm:</p>
 * <ol>
 *   <li>Sau khi sinh viên đăng nhập thành công (chạy nền, không chặn login).</li>
 *   <li>Sinh viên bấm "Kiểm tra lại" trên trang kết quả thanh toán
 *       (POST /api/v1/student/payments/sync, có throttle chống gọi dày).</li>
 * </ol>
 */
@Slf4j
@Service
public class SepayPaymentSyncService {

    private static final int MAX_THROTTLE_ENTRIES = 10_000;

    private final SepayProperties properties;
    private final SepayWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<Long, Instant> lastSyncAtByUser = new ConcurrentHashMap<>();
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sepay-payment-sync");
        thread.setDaemon(true);
        return thread;
    });

    public SepayPaymentSyncService(SepayProperties properties,
                                   SepayWebhookService webhookService,
                                   ObjectMapper objectMapper) {
        this.properties = properties;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /** Dừng thread nền khi ứng dụng shutdown. */
    @PreDestroy
    public void shutdown() {
        syncExecutor.shutdownNow();
    }

    /** Gọi ngay sau khi sinh viên đăng nhập — chạy nền, không bao giờ làm hỏng login. */
    public void syncInBackground(Long userId) {
        if (userId == null || userId <= 0 || !isEnabled()) {
            return;
        }
        try {
            syncExecutor.submit(() -> {
                try {
                    syncForUser(userId, false);
                } catch (Exception error) {
                    log.warn("Đối soát SePay nền cho user #{} lỗi: {}", userId, error.getMessage());
                }
            });
        } catch (Exception error) {
            log.warn("Không gửi được tác vụ đối soát SePay: {}", error.getMessage());
        }
    }

    /**
     * Chạy một lượt đối soát cho user (đồng bộ). Kết quả KHÔNG bao giờ ném lỗi
     * ra ngoài — lỗi mạng/API chỉ được ghi log để login và trang thanh toán
     * không bao giờ vỡ vì SePay.
     *
     * @param force true để bỏ throttle (dùng cho nút "Kiểm tra lại")
     */
    public PaymentSyncResponse syncForUser(Long userId, boolean force) {
        if (!isEnabled()) {
            return new PaymentSyncResponse(false, 0, 0, 0);
        }
        if (!force && !throttleAllows(userId)) {
            return new PaymentSyncResponse(true, 0, 0, 1);
        }

        SepayProperties.Transactions config = properties.getTransactions();
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(buildListUrl(config)))
                    .timeout(Duration.ofSeconds(Math.max(1, config.getTimeoutSeconds())))
                    .header("Authorization", "Apikey " + properties.getApiKey().trim())
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception error) {
            log.warn("Không gọi được SePay transactions API: {}", error.getMessage());
            return new PaymentSyncResponse(true, 0, 0, 0);
        }

        if (response.statusCode() != 200) {
            log.warn("SePay transactions API trả HTTP {} — kiểm tra dormhub.sepay.api-key",
                    response.statusCode());
            return new PaymentSyncResponse(true, 0, 0, 0);
        }

        int checked = 0;
        int matched = 0;
        try {
            JsonNode transactions = objectMapper.readTree(response.body()).path("transactions");
            if (transactions.isArray()) {
                for (JsonNode item : transactions) {
                    SepayWebhookPayload payload = toPayload(item);
                    if (payload == null) {
                        continue;
                    }
                    checked++;
                    try {
                        if (webhookService.reconcile(payload) == Outcome.MATCHED) {
                            matched++;
                            log.info("Đối soát SePay: giao dịch #{} khớp thanh toán mã {}",
                                    payload.getId(), payload.getCode());
                        }
                    } catch (RuntimeException error) {
                        if (!webhookService.isTransactionIdUniqueViolation(error)) {
                            log.warn("Đối soát giao dịch SePay #{} lỗi: {}",
                                    payload.getId(), error.getMessage());
                        }
                    }
                }
            }
        } catch (Exception error) {
            log.warn("Phân tích phản hồi SePay transactions API lỗi: {}", error.getMessage());
        }

        if (checked > 0) {
            log.info("Đối soát SePay cho user #{}: {}/{} giao dịch khớp", userId, matched, checked);
        }
        return new PaymentSyncResponse(true, checked, matched, 0);
    }

    /** Biến một giao dịch SePay (amountIn > 0) thành payload chuẩn webhook. */
    private SepayWebhookPayload toPayload(JsonNode item) {
        BigDecimal amountIn = item.path("amountIn").decimalValue();
        if (amountIn == null || amountIn.signum() <= 0) {
            return null; // giao dịch tiền ra — bỏ qua
        }
        long id = item.path("id").asLong(0);
        if (id <= 0) {
            return null;
        }

        SepayWebhookPayload payload = new SepayWebhookPayload();
        payload.setId(id);
        payload.setGateway(textOrNull(item, "gateway"));
        payload.setTransactionDate(textOrNull(item, "transactionDate"));
        payload.setAccountNumber(textOrNull(item, "accountNumber"));

        // SePay tách "mã giao dịch" từ nội dung chuyển khoản; nếu API không trả
        // code thì tự lấy token đầu tiên của nội dung (QR của hệ thống đặt mã DH... đứng đầu).
        String code = item.path("code").asText("");
        if (code == null || code.isBlank()) {
            String content = item.path("transactionContent").asText("");
            String[] tokens = content == null ? new String[0] : content.trim().split("\\s+");
            code = tokens.length > 0 ? tokens[0] : "";
        }
        payload.setCode(code == null || code.isBlank() ? null : code.trim());
        payload.setContent(textOrNull(item, "transactionContent"));
        payload.setTransferType("in");
        payload.setTransferAmount(amountIn);
        payload.setReferenceCode(textOrNull(item, "referenceNumber"));
        return payload;
    }

    private String buildListUrl(SepayProperties.Transactions config) {
        StringBuilder url = new StringBuilder(config.getUrl());
        String separator = url.toString().contains("?") ? "&" : "?";
        if (config.getAccountNumber() != null && !config.getAccountNumber().isBlank()) {
            url.append(separator)
                    .append("account_number=")
                    .append(URLEncoder.encode(config.getAccountNumber().trim(), StandardCharsets.UTF_8));
            separator = "&";
        }
        url.append(separator).append("limit=").append(Math.max(1, config.getLimit()));
        return url.toString();
    }

    private boolean isEnabled() {
        SepayProperties.Transactions config = properties.getTransactions();
        return config != null && config.isEnabled()
                && properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    private boolean throttleAllows(Long userId) {
        if (lastSyncAtByUser.size() > MAX_THROTTLE_ENTRIES) {
            lastSyncAtByUser.clear();
        }
        Instant now = Instant.now();
        Instant previous = lastSyncAtByUser.put(userId, now);
        return previous == null
                || previous.plusMillis(Math.max(0, properties.getSyncMinIntervalSeconds()) * 1000L)
                        .isBefore(now);
    }

    private String textOrNull(JsonNode item, String field) {
        JsonNode value = item.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText("").trim();
        return text.isEmpty() ? null : text;
    }
}
