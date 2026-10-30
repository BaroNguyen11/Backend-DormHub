package com.dormhub.mapper;

import com.dormhub.dto.response.StudentProfileResponse;
import com.dormhub.entity.Student;
import com.dormhub.entity.User;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentProfileResponse toResponse(Student student) {
        if (student == null) {
            return null;
        }

        User user = student.getUser();

        return StudentProfileResponse.builder()
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .avatarUrl(student.getAvatarUrl())
                .cic(student.getCic())
                // Thông tin lấy từ User liên kết (có null check an toàn)
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .fullName(user != null ? user.getFullName() : null)
                .phone(user != null ? user.getPhone() : null)
                .build();
    }
}