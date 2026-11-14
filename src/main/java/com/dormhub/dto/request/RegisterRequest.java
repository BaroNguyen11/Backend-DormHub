package com.dormhub.dto.request;


import com.fasterxml.jackson.annotation.JsonAlias;
import com.dormhub.entity.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 30,
            message = "Username phải từ 3 đến 30 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 8, max = 72,
            message = "Password phải từ 8 đến 72 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @JsonAlias("phone")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$",
            message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @NotBlank(message = "Mã sinh viên không được để trống")
    @Pattern(regexp = "^[0-9]{8}$", message = "Mã sinh viên phải gồm đúng 8 chữ số")
    private String studentCode;

//    @NotNull(message = "Giới tính không được để trống")
//    private Gender gender;
}
