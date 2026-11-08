package com.dormhub.service;

import com.dormhub.entity.Room;
import com.dormhub.entity.Student;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.DormApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomApplicationAvailabilityService {

    private static final List<ContractStatus> PENDING_CONTRACT = List.of(ContractStatus.PENDING);
    private static final List<DormApplicationStatus> PENDING_APPLICATION = List.of(DormApplicationStatus.PENDING);
    private static final List<DormApplicationStatus> ACTIVE_APPLICATIONS =
            List.of(DormApplicationStatus.PENDING, DormApplicationStatus.APPROVED);

    private final ContractRepository contractRepository;
    private final DormApplicationRepository dormApplicationRepository;

    public boolean hasActiveApplication(Student student) {
        return dormApplicationRepository.existsByStudentIdAndStatusIn(student.getId(), ACTIVE_APPLICATIONS);
    }

    public boolean hasRequiredProfileFields(Student student) {
        return StringUtils.hasText(student.getStudentCode())
                && student.getDateOfBirth() != null
                && StringUtils.hasText(student.getAddress());
    }

    public boolean hasCompleteProfile(Student student) {
        return hasRequiredProfileFields(student)
                && student.getCic() != null
                && student.getCic().matches("[0-9]{12}")
                && (student.getGender() == Gender.MALE || student.getGender() == Gender.FEMALE);
    }

    public boolean matchesBuildingGender(Room room, Student student) {
        return (student.getGender() == Gender.MALE || student.getGender() == Gender.FEMALE)
                && room.getBuilding().getGenderRestriction() != null
                && room.getBuilding().getGenderRestriction() == student.getGender();
    }

    public Map<Long, Long> occupiedAndHeld(List<Room> rooms) {
        if (rooms.isEmpty()) return Map.of();
        Collection<Long> roomIds = rooms.stream().map(Room::getId).toList();
        Map<Long, Long> pendingContracts = contractRepository.findRoomHoldCounts(roomIds, PENDING_CONTRACT)
                .stream().collect(Collectors.toMap(
                        ContractRepository.RoomHoldCountProjection::getRoomId,
                        ContractRepository.RoomHoldCountProjection::getHoldCount));
        Map<Long, Long> pendingApplications = dormApplicationRepository.findRoomHoldCounts(roomIds, PENDING_APPLICATION)
                .stream().collect(Collectors.toMap(
                        DormApplicationRepository.RoomHoldCountProjection::getRoomId,
                        DormApplicationRepository.RoomHoldCountProjection::getHoldCount));
        return rooms.stream().collect(Collectors.toMap(Room::getId,
                room -> room.getCurrentOccupancy().longValue()
                        + pendingContracts.getOrDefault(room.getId(), 0L)
                        + pendingApplications.getOrDefault(room.getId(), 0L)));
    }

    public long remainingSlots(Room room, long occupiedAndHeld) {
        return Math.max(0L, room.getCapacity().longValue() - occupiedAndHeld);
    }

    public boolean roomAcceptsApplications(Room room, long occupiedAndHeld) {
        return room.getStatus() == RoomStatus.AVAILABLE && remainingSlots(room, occupiedAndHeld) > 0;
    }

    public void validateRoomForApplication(Room room, Student student) {
        if (student.getGender() != Gender.MALE && student.getGender() != Gender.FEMALE) {
            throw new AppException(ErrorCode.DORM_APPLICATION_GENDER_UNSUPPORTED);
        }
        if (room.getBuilding().getGenderRestriction() == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
        if (!matchesBuildingGender(room, student)) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_MISMATCH);
        }
        long occupiedAndHeld = room.getCurrentOccupancy().longValue()
                + contractRepository.countByRoom_IdAndStatusIn(room.getId(), PENDING_CONTRACT)
                + dormApplicationRepository.countByRequestedRoomIdAndStatusIn(room.getId(), PENDING_APPLICATION);
        if (remainingSlots(room, occupiedAndHeld) == 0) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE);
        }
    }
}
