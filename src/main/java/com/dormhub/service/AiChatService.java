
package com.dormhub.service;

import com.dormhub.dto.request.AiChatRequest;
import com.dormhub.entity.User;
import com.dormhub.repository.UserRepository;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final UserRepository userRepository;

    @Value("${groq.api-key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${groq.model:${GROQ_MODEL:openai/gpt-oss-120b}}")
    private String defaultModel;

    @Value("${groq.api-url:${GROQ_API_URL:https://api.groq.com/openai/v1/chat/completions}}")
    private String groqApiUrl;

    public String chat(
            AiChatRequest request,
            String currentUserEmail,
            String currentUserRole
    ) {
        String apiKey = resolveValue(
                groqApiKey,
                System.getenv("GROQ_API_KEY")
        );

        if (apiKey == null || apiKey.isBlank()) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Hệ thống AI chưa được cấu hình GROQ_API_KEY ở phía Server."
            );
        }

        // 1. Lấy thông tin chi tiết User từ DB để làm ngữ cảnh cho AI
        User user = userRepository.findByEmail(currentUserEmail).orElse(null);
        String userName = user != null ? user.getFullName() : "Người dùng";

        StringBuilder userContextBuilder = new StringBuilder();
        userContextBuilder.append(
                String.format("- Họ tên: %s\n- Vai trò: %s\n",
                        userName, currentUserRole)
        );

        if ("STUDENT".equalsIgnoreCase(currentUserRole) && user != null) {
            userContextBuilder.append("- Định hướng link cho sinh viên:\n");
            userContextBuilder.append(
                    "  + Xem hóa đơn/thanh toán: [Quản lý hóa đơn](/student/invoices)\n"
            );
            userContextBuilder.append(
                    "  + Xem chỗ ở & hợp đồng: [Chỗ ở & Hợp đồng](/student/residence)\n"
            );
            userContextBuilder.append(
                    "  + Báo hỏng thiết bị: [Báo hỏng / Sửa chữa](/student/maintenance)\n"
            );
        } else if ("MANAGER".equalsIgnoreCase(currentUserRole)) {
            userContextBuilder.append("- Định hướng link cho Quản lý:\n");
            userContextBuilder.append(
                    "  + Quản lý phòng & tòa nhà: [Phòng & Tòa nhà](/manager/rooms)\n"
            );
            userContextBuilder.append(
                    "  + Quản lý hóa đơn: [Tài chính & Hóa đơn](/manager/invoices)\n"
            );
            userContextBuilder.append(
                    "  + Duyệt đơn đăng ký: [Sinh viên & Đơn](/manager/students)\n"
            );
        }

        // 2. Xây dựng System Prompt thông minh có kèm khả năng điều hướng URL (Deep Linking nghiêm ngặt)
        String systemPrompt = String.format("""
            Bạn là trợ lý AI thông minh, chuyên nghiệp của hệ thống ký túc xá DormHub.
            
            QUY TẮC ĐẶC BIỆT VỀ ĐIỀU HƯỚNG (DEEP LINKING - BẮT BUỘC TUÂN THỦ):
            - Khi người dùng hỏi về hóa đơn, tiền phòng, hợp đồng, phòng ở, sửa chữa,... NGOÀI việc giải đáp, BẮT BUỘC phải chèn đường dẫn Markdown chính xác để hướng dẫn họ truy cập vào trang tương ứng.
            - CHỈ ĐƯỢC PHÉP sử dụng C EXACT CÁC ĐƯỜNG DẪN NỘI BỘ SAU ĐÂY. TUYỆT ĐỐI CẤM tự bịa ra các URL khác như /billing, /payment, /settings,...:
              + Hóa đơn & Thanh toán của sinh viên: `/student/invoices`
              + Chỗ ở & Hợp đồng của sinh viên: `/student/residence`
              + Báo hỏng / Sửa chữa của sinh viên: `/student/maintenance`
              + Quản lý phòng & tòa nhà: `/manager/rooms`
              + Tài chính & Hóa đơn của quản lý: `/manager/invoices`
              + Quản lý sinh viên & đơn: `/manager/students`
            
            THÔNG TIN NGỮ CẢNH CỦA NGƯỜI DÙNG HIỆN TẠI:
            %s
            
            Định dạng Markdown bắt buộc:
            - Dùng **bold** cho từ khóa quan trọng.
            - Dùng danh sách (1, 2, 3 hoặc gạch đầu dòng) cho các bước hướng dẫn.
            - Luôn đính kèm link dạng `[Tên hiển thị](/đường-dẫn-chính-xác-ở-trên)` khi nhắc tới chức năng.
            """, userContextBuilder.toString());

        // 3. Chuẩn bị danh sách message gửi sang Groq API
        List<Map<String, Object>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", systemPrompt
        ));

        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            request.getMessages().stream()
                    .filter(m -> !"system".equalsIgnoreCase(
                            String.valueOf(m.get("role"))
                    ))
                    .forEach(messages::add);

        } else if (request.getMessage() != null && !request.getMessage().isBlank()) {
            messages.add(Map.of(
                    "role", "user",
                    "content", request.getMessage().trim()
            ));

        } else {
            throw new AppException(
                    ErrorCode.VALIDATION_ERROR,
                    "Nội dung tin nhắn không được để trống"
            );
        }

        // 4. Resolve model & URL
        String model = resolveValue(
                request.getModel(),
                resolveValue(defaultModel, "openai/gpt-oss-120b")
        );

        String fullUrl = resolveValue(
                groqApiUrl,
                "https://api.groq.com/openai/v1/chat/completions"
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put(
                "temperature",
                request.getTemperature() != null
                        ? request.getTemperature()
                        : 0.7
        );
        payload.put(
                "max_tokens",
                request.getMaxTokens() != null
                        ? request.getMaxTokens()
                        : 2000
        );
        payload.put("stream", false);

        RestClient client = RestClient.builder()
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();

        try {
            Map<String, Object> response = client.post()
                    .uri(fullUrl)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey
                    )
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("choices")) {
                List<?> choices = (List<?>) response.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                    Map<?, ?> msg = (Map<?, ?>) firstChoice.get("message");

                    if (msg != null && msg.get("content") != null) {
                        return msg.get("content").toString();
                    }
                }
            }

            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Phản hồi từ AI không hợp lệ."
            );

        } catch (RestClientResponseException ex) {
            String body = ex.getResponseBodyAsString();

            log.error(
                    "AI Chat Error HTTP {}: {}",
                    ex.getStatusCode().value(),
                    body
            );

            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "AI trả về lỗi: " + ex.getStatusCode().value()
            );

        } catch (Exception ex) {
            log.error(
                    "AI Connection Error: {}",
                    ex.getMessage(),
                    ex
            );

            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    "Không thể kết nối đến máy chủ AI: " + ex.getMessage()
            );
        }
    }

    private static String resolveValue(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }

        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }

        return null;
    }
}
