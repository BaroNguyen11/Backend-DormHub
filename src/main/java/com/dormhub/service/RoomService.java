package com.dormhub.service;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.BuildingGenderPolicy;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.DormApplicationRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final DormApplicationRepository dormApplicationRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomMapper roomMapper;

    public Page<RoomResponse> search(
            Long buildingId,
            BuildingGenderPolicy buildingGenderPolicy,
            String roomType,
            RoomStatus status,
            boolean availableOnly,
            String keyword,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "page phải >= 0 và size phải từ 1 đến " + MAX_PAGE_SIZE);
        }

        Specification<Room> specification = (root, query, criteriaBuilder) -> {
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("building", JoinType.INNER);
                query.distinct(true);
            }

            var predicates = criteriaBuilder.conjunction();

            if (buildingId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("building").get("id"), buildingId));
            }
            if (buildingGenderPolicy != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(
                                root.get("building").get("genderPolicy"), buildingGenderPolicy));
            }
            if (StringUtils.hasText(roomType)) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(
                                criteriaBuilder.lower(root.get("roomType")),
                                roomType.trim().toLowerCase()));
            }
            if (status != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(keyword)) {
                var buildingJoin = root.join("building", JoinType.INNER);
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("roomNumber")), pattern),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(buildingJoin.get("buildingName")), pattern)));
            }
            if (availableOnly) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("status"), RoomStatus.AVAILABLE),
                        criteriaBuilder.lt(
                                criteriaBuilder.sum(root.<Integer>get("currentOccupancy"),
                                        root.<Integer>get("reservedOccupancy")),
                                root.<Integer>get("capacity")));
            }
            return predicates;
        };

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("building.buildingName"), Sort.Order.asc("roomNumber")));

        return withAvailableSlots(roomRepository.findAll(specification, pageable));
    }

    public RoomResponse getById(Long id) {
        Room room = findRoom(id);
        return toResponse(room);
    }

    @Transactional
    public RoomResponse create(RoomRequest request) {
        Building building = findBuilding(request.getBuildingId());
        requireGenderPolicy(building);
        String roomNumber = request.getRoomNumber().trim();

        if (roomRepository.existsByBuildingIdAndRoomNumber(building.getId(), roomNumber)) {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        Room room = new Room();
        room.setBuilding(building);
        room.setCurrentOccupancy(0);
        room.setReservedOccupancy(0);
        apply(room, request, roomNumber);
        return toResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Building building = findBuilding(request.getBuildingId());
        requireGenderPolicy(building);
        String roomNumber = request.getRoomNumber().trim();
        int currentOccupancy = room.getCurrentOccupancy();
        int totalOccupiedOrReserved = currentOccupancy + room.getReservedOccupancy();

        if (request.getCapacity() < totalOccupiedOrReserved) {
            throw new AppException(ErrorCode.ROOM_CAPACITY_BELOW_OCCUPANCY);
        }
        if (request.getStatus() == RoomStatus.AVAILABLE
                && request.getCapacity() <= totalOccupiedOrReserved) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        if (room.getReservedOccupancy() > 0
                && (request.getStatus() == RoomStatus.MAINTENANCE
                || request.getStatus() == RoomStatus.INACTIVE)) {
            throw new AppException(ErrorCode.ROOM_HAS_ACTIVE_CONTRACTS);
        }
        if (!room.getBuilding().getId().equals(building.getId())
                && (totalOccupiedOrReserved > 0 || roomAssignmentRepository.existsByRoomIdAndStatus(
                        id, RoomAssignmentStatus.ASSIGNED))) {
            throw new AppException(ErrorCode.ROOM_HAS_ACTIVE_ASSIGNMENTS);
        }
        if (!room.getBuilding().getId().equals(building.getId())
                && dormApplicationRepository.existsByRequestedRoomIdAndStatusIn(
                        id, List.of(DormApplicationStatus.PENDING, DormApplicationStatus.APPROVED))) {
            throw new AppException(ErrorCode.ROOM_HAS_ACTIVE_APPLICATIONS);
        }

        if (roomRepository.existsByBuildingIdAndRoomNumberAndIdNot(
                building.getId(), roomNumber, id)) {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        room.setBuilding(building);
        apply(room, request, roomNumber);
        return toResponse(roomRepository.save(room));
    }

    @Transactional
    public void delete(Long id) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (roomAssignmentRepository.existsByRoomId(id)
                || dormApplicationRepository.existsByRequestedRoomId(id)) {
            throw new AppException(ErrorCode.ROOM_HAS_HISTORY);
        }
        roomRepository.delete(room);
    }

    private void apply(Room room, RoomRequest request, String roomNumber) {
        room.setRoomNumber(roomNumber);
        room.setFloor(request.getFloor());
        room.setCapacity(request.getCapacity());
        room.setRoomType(request.getRoomType().trim());
        room.setPrice(request.getPrice());
        room.setStatus(request.getStatus());
        room.setDescription(request.getDescription());
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
    }

    private void requireGenderPolicy(Building building) {
        if (building.getGenderPolicy() == null) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
    }

    private Page<RoomResponse> withAvailableSlots(Page<Room> rooms) {
        return rooms.map(this::toResponse);
    }

    private RoomResponse toResponse(Room room) {
        int availableSlots = room.getStatus() == RoomStatus.AVAILABLE
                ? Math.max(0, room.getCapacity() - room.getCurrentOccupancy() - room.getReservedOccupancy())
                : 0;
        return roomMapper.toResponse(room, availableSlots);
    }
}
