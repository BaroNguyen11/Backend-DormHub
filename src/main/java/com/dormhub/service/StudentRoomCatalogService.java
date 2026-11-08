package com.dormhub.service;

import com.dormhub.dto.response.StudentRoomResponse;
import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.RoomRepository;
import com.dormhub.repository.StudentRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentRoomCatalogService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final RoomApplicationAvailabilityService availabilityService;

    public Page<StudentRoomResponse> getRooms(Long userId, String search, RoomStatus status,
                                              RoomType roomType, Long buildingId, int page, int size) {
        if (page < 0 || size < 1 || size > 100 || (buildingId != null && buildingId < 1)
                || (search != null && search.length() > 100)) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        Student student = findStudent(userId);
        String term = StringUtils.hasText(search) ? search.trim().toLowerCase(Locale.ROOT) : null;
        Specification<Room> specification = (root, criteriaQuery, criteriaBuilder) -> {
            if (criteriaQuery != null && criteriaQuery.getResultType() != Long.class) {
                root.fetch("building", JoinType.INNER);
            }
            var predicate = criteriaBuilder.conjunction();
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            if (roomType != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("roomType"), roomType));
            }
            if (buildingId != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("building").get("id"), buildingId));
            }
            if (term != null) {
                String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%")
                        .replace("_", "\\_") + "%";
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("roomNumber")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("building").get("name")), pattern, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("building").get("buildingCode")), pattern, '\\')));
            }
            return predicate;
        };
        Page<Room> rooms = roomRepository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        Map<Long, Long> occupiedAndHeld = availabilityService.occupiedAndHeld(rooms.getContent());
        boolean studentCanApply = availabilityService.hasCompleteProfile(student)
                && !availabilityService.hasActiveApplication(student);
        return rooms.map(room -> toResponse(room, student, studentCanApply,
                occupiedAndHeld.get(room.getId())));
    }

    public StudentRoomResponse getRoom(Long userId, Long roomId) {
        Student student = findStudent(userId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        List<Room> rooms = List.of(room);
        long occupiedAndHeld = availabilityService.occupiedAndHeld(rooms).get(roomId);
        boolean studentCanApply = availabilityService.hasCompleteProfile(student)
                && !availabilityService.hasActiveApplication(student);
        return toResponse(room, student, studentCanApply, occupiedAndHeld);
    }

    private Student findStudent(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));
    }

    private StudentRoomResponse toResponse(Room room, Student student, boolean studentCanApply,
                                           long occupiedAndHeld) {
        boolean available = availabilityService.roomAcceptsApplications(room, occupiedAndHeld);
        return StudentRoomResponse.builder()
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .buildingId(room.getBuilding().getId())
                .buildingName(room.getBuilding().getName())
                .buildingCode(room.getBuilding().getBuildingCode())
                .buildingAddress(room.getBuilding().getAddress())
                .buildingGenderRestriction(room.getBuilding().getGenderRestriction())
                .roomType(room.getRoomType())
                .capacity(room.getCapacity())
                .currentOccupancy(room.getCurrentOccupancy())
                .remainingSlots(availabilityService.remainingSlots(room, occupiedAndHeld))
                .pricePerMonth(room.getPricePerMonth())
                .status(room.getStatus())
                .description(room.getDescription())
                .availableForApplication(available)
                .eligibleForStudent(available && studentCanApply
                        && availabilityService.matchesBuildingGender(room, student))
                .build();
    }
}
