
package com.dormhub.service;

import com.dormhub.dto.request.MaintenanceCreateRequest;
import com.dormhub.dto.request.MaintenanceStatusUpdateRequest;
import com.dormhub.dto.response.MaintenanceResponse;
import com.dormhub.entity.MaintenanceRequest;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.MaintenanceStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.MaintenanceMapper;
import com.dormhub.repository.MaintenanceRepository;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final MaintenanceMapper maintenanceMapper;

    private Student getStudentByUserId(Long userId) {
        return studentRepository.findByUserId(userId).orElseThrow(() ->
                new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }
    @Transactional
    public MaintenanceResponse create(MaintenanceCreateRequest request, Long userId) {
        Student student = getStudentByUserId(userId);

        Room room = roomRepository.findById(request.getRoomId()).orElseThrow(() ->
                        new AppException(ErrorCode.ROOM_NOT_FOUND));

        MaintenanceRequest maintenance = MaintenanceRequest.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .maintenanceStatus(MaintenanceStatus.PENDING)
                .student(student)
                .room(room)
                .build();

        MaintenanceRequest saved = maintenanceRepository.save(maintenance);

        return maintenanceMapper.toResponse(saved);
    }



    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getMyRequests(Long userId) {
        Student student = getStudentByUserId(userId);
        return maintenanceRepository
                .findAllByStudent_IdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(maintenanceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceResponse getMyRequest(Long maintenanceId, Long userId) {
        Student student = getStudentByUserId(userId);
        MaintenanceRequest maintenance = maintenanceRepository
                .findByIdAndStudent_Id(maintenanceId, student.getId())
                .orElseThrow(() ->
                        new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        return maintenanceMapper.toResponse(maintenance);
    }

    @Transactional
    public MaintenanceResponse updateStatus(Long maintenanceId, MaintenanceStatusUpdateRequest request) {
        MaintenanceRequest maintenance = maintenanceRepository
                .findById(maintenanceId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.MAINTENANCE_NOT_FOUND));

        MaintenanceStatus currentStatus = maintenance.getMaintenanceStatus();
        MaintenanceStatus newStatus = request.getStatus();

        // Không cần cập nhật nếu trạng thái không thay đổi
        if (currentStatus == newStatus) {
            return maintenanceMapper.toResponse(maintenance);
        }

        // Không cho thay đổi yêu cầu đã kết thúc
        if (currentStatus == MaintenanceStatus.COMPLETED || currentStatus == MaintenanceStatus.REJECTED) {
            throw new AppException(ErrorCode.MAINTENANCE_INVALID_STATUS);
        }

        // PENDING chỉ được chuyển sang IN_PROGRESS hoặc REJECTED
        if (currentStatus == MaintenanceStatus.PENDING
                && newStatus != MaintenanceStatus.IN_PROGRESS
                && newStatus != MaintenanceStatus.REJECTED) {
            throw new AppException(ErrorCode.MAINTENANCE_INVALID_STATUS);
        }

        // IN_PROGRESS chỉ được chuyển sang COMPLETED hoặc REJECTED
        if (currentStatus == MaintenanceStatus.IN_PROGRESS
                && newStatus != MaintenanceStatus.COMPLETED
                && newStatus != MaintenanceStatus.REJECTED) {
            throw new AppException(ErrorCode.MAINTENANCE_INVALID_STATUS);
        }

        maintenance.setMaintenanceStatus(newStatus);

        if (newStatus == MaintenanceStatus.COMPLETED) {
            maintenance.setResolvedAt(LocalDateTime.now());
        }

        MaintenanceRequest updated = maintenanceRepository.save(maintenance);

        return maintenanceMapper.toResponse(updated);
    }



}
