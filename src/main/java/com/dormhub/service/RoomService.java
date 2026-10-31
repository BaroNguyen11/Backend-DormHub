package com.dormhub.service;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.RoomAssignment;
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
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
import java.util.Map;
import java.util.stream.Collectors;

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
                Subquery<Long> occupiedSlots = query.subquery(Long.class);
                Root<RoomAssignment> assignment = occupiedSlots.from(RoomAssignment.class);
                occupiedSlots.select(criteriaBuilder.count(assignment));
                occupiedSlots.where(
                        criteriaBuilder.equal(assignment.get("room").get("id"), root.get("id")),
                        criteriaBuilder.equal(assignment.get("status"), RoomAssignmentStatus.ASSIGNED));
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("status"), RoomStatus.ACTIVE),
                        criteriaBuilder.gt(root.<Integer>get("capacity"), occupiedSlots));
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
        return toResponse(room, roomAssignmentRepository.countByRoomIdAndStatus(
                id, RoomAssignmentStatus.ASSIGNED));
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
        apply(room, request, roomNumber);
        return toResponse(roomRepository.save(room), 0);
    }

    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Building building = findBuilding(request.getBuildingId());
        requireGenderPolicy(building);
        String roomNumber = request.getRoomNumber().trim();
        long assignedCount = roomAssignmentRepository.countByRoomIdAndStatus(
                id, RoomAssignmentStatus.ASSIGNED);

        if (request.getCapacity() < assignedCount) {
            throw new AppException(ErrorCode.ROOM_CAPACITY_BELOW_OCCUPANCY);
        }
        if (!room.getBuilding().getId().equals(building.getId()) && assignedCount > 0) {
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
        return toResponse(roomRepository.save(room), assignedCount);
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
        List<Long> roomIds = rooms.getContent().stream().map(Room::getId).toList();
        Map<Long, Long> assignedCounts = roomIds.isEmpty()
                ? Map.of()
                : roomAssignmentRepository.countByRoomIdsAndStatus(roomIds, RoomAssignmentStatus.ASSIGNED)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> ((Number) row[0]).longValue(),
                                row -> ((Number) row[1]).longValue()));

        return rooms.map(room -> toResponse(room, assignedCounts.getOrDefault(room.getId(), 0L)));
    }

    private RoomResponse toResponse(Room room, long assignedCount) {
        int availableSlots = room.getStatus() == RoomStatus.ACTIVE
                ? Math.max(0, room.getCapacity() - (int) assignedCount)
                : 0;
        return roomMapper.toResponse(room, availableSlots);
    }
}
