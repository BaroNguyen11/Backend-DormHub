package com.dormhub.controller;

import com.dormhub.dto.response.ApiResponse;
import com.dormhub.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class UploadController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        // 1. Kiểm tra tệp rỗng
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), "Vui lòng chọn tệp hình ảnh"));
        }
        // 2. Kiểm tra dung lượng tệp (10MB = 10 * 1024 * 1024 bytes)
        long maxSizeBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error(String.valueOf(HttpStatus.PAYLOAD_TOO_LARGE.value()), "Dung lượng ảnh vượt quá giới hạn 10 MB"));
        }
        // 3. Kiểm tra định dạng (Content-Type)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), "Tệp tải lên không phải là hình ảnh hợp lệ"));
        }
        // 4. Tiến hành upload lên Cloudinary và trả về theo chuẩn ApiResponse của hệ thống
        String imageUrl = cloudinaryService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công", Map.of("url", imageUrl)));
    }
}