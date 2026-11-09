package com.dormhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tích hợp SePay (map từ application.yml, prefix "dormhub.sepay").
 *
 * <pre>
 * dormhub:
 *   sepay:
 *     api-key: &lt;API Key trong my.sepay.vn -&gt; Cài đặt -&gt; API&gt;   # BẮT BUỘC cho webhook/sync thật
 *     webhook-secret: ""            # (tuỳ chọn) chỉ dùng khi tự bắn webhook có chữ ký HMAC
 *     sync-min-interval-seconds: 10 # chống gọi SePay API quá dày
 *     transactions:
 *       enabled: true
 *       url: https://my.sepay.vn/userapi/transactions/list
 *       account-number: &lt;số tài khoản đăng ký với SePay&gt;      # lọc giao dịch vào đúng TK
 *       limit: 30                  # số giao dịch gần nhất được kéo về đối soát
 *       timeout-seconds: 5
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "dormhub.sepay")
public class SepayProperties {

    /**
     * API Key của tài khoản SePay (my.sepay.vn -> Cài đặt -> API).
     * SePay gửi webhook kèm header "Authorization: Apikey <API_KEY>" —
     * backend so khớp với giá trị này để xác thực request.
     */
    private String apiKey = "";

    /**
     * (Tuỳ chọn) Secret cho cơ chế HMAC tự chế cũ (X-SePay-Signature +
     * X-SePay-Timestamp). Chỉ dùng khi bạn tự bắn webhook từ script kiểm thử.
     */
    private String webhookSecret = "";

    /** Khoảng cách tối thiểu giữa hai lượt pull giao dịch theo từng user (giây). */
    private long syncMinIntervalSeconds = 10;

    private final Transactions transactions = new Transactions();

    @Data
    public static class Transactions {

        /** Bật/tắt đối soát chủ động (pull). Webhook vẫn hoạt động riêng. */
        private boolean enabled = true;

        /** Endpoint danh sách giao dịch của SePay (giao dịch cá nhân v1). */
        private String url = "https://my.sepay.vn/userapi/transactions/list";

        /** Số tài khoản đăng ký nhận tiền với SePay (lọc phía API). */
        private String accountNumber = "";

        /** Số giao dịch gần nhất tối đa được kéo về mỗi lượt. */
        private int limit = 30;

        /** Timeout từng request HTTP tới SePay (giây). */
        private int timeoutSeconds = 5;
    }
}
