package com.dormhub.dto.request;

import com.dormhub.entity.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StudentProfileUpdateRequest {

    @Past(message = "Ngày sinh phải nhỏ hơn ngày hiện tại")
    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @Size(max = 500, message = "URL ảnh đại diện không được vượt quá 500 ký tự")
    @Pattern(
            regexp = "^https?://\\S+$",
            message = "URL ảnh đại diện phải bắt đầu bằng http:// hoặc https://"
    )
    private String avatarUrl;

    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Pattern(regexp = "^[0-9]{12}$", message = "Số CCCD phải gồm đúng 12 chữ số")
    private String cic;
}
