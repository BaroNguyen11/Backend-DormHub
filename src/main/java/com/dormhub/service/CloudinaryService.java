package com.dormhub.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Vui lòng chọn ảnh cần tải lên.");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Ảnh không được vượt quá 10 MB.");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tệp tải lên phải là hình ảnh.");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("folder", "dormhub/maintenance", "resource_type", "image")
            );
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Cloudinary không trả về URL ảnh.");
            }
            return secureUrl.toString();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tải ảnh lên Cloudinary.");
        }
    }
}
