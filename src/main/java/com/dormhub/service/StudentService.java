
package com.dormhub.service;

import com.dormhub.dto.request.StudentProfileUpdateRequest;
import com.dormhub.dto.response.StudentProfileResponse;
import com.dormhub.entity.Student;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentProfileResponse getMyProfile(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.STUDENT_NOT_FOUND));

        return StudentProfileResponse.builder()
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .avatarUrl(student.getAvatarUrl())
                .username(student.getUser().getUsername())
                .email(student.getUser().getEmail())
                .fullName(student.getUser().getFullName())
                .build();
    }

    @Transactional
    public StudentProfileResponse updateMyProfile(
            Long userId,
            StudentProfileUpdateRequest request) {

        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.STUDENT_NOT_FOUND));

        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }

        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }

        if (request.getAvatarUrl() != null) {
            student.setAvatarUrl(request.getAvatarUrl());
        }

        studentRepository.save(student);

        return getMyProfile(userId);
    }

}
