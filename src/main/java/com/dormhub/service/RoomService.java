package com.dormhub.service;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.DormApplicationRepository;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.RoomRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<ContractStatus> CAPACITY_HOLD_STATUSES = List.of(ContractStatus.PENDING);

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;
    private final DormApplicationRepository dormApplicationRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomMapper roomMapper;

    public Page<RoomResponse> getAllRooms(RoomStatus status, RoomType roomType,
                                          Long buildingId, Pageable pageable) {
        validatePageable(pageable);
        Specification<Room> specification = (root, query, criteriaBuilder) -> {
            if (query != null && query.getResultType() != Long.class) {
                root.fetch("building", JoinType.LEFT);
                query.distinct(true);
            }
            var predicates = criteriaBuilder.conjunction();
            if (status != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("status"), status));
            }
            if (roomType != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("roomType"), roomType));
            }
            if (buildingId != null) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.equal(root.get("building").get("id"), buildingId));
            }
            return predicates;
        };
        Page<Room> rooms = roomRepository.findAll(specification, pageable);
        Map<Long, Integer> holdsByRoom = getPendingContractCounts(rooms.getContent());
        return rooms.map(room -> toResponseWithAvailability(
                room, holdsByRoom.getOrDefault(room.getId(), 0)));
    }

    public RoomResponse getRoomById(Long id) {
        Room room = findRoom(id);
        return toResponseWithAvailability(room, getPendingContractCount(room.getId()));
    }

    public List<RoomResponse> getAvailableRooms() {
        List<Room> rooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        Map<Long, Integer> holdsByRoom = getPendingContractCounts(rooms);
        return rooms.stream()
                .map(room -> toResponseWithAvailability(room, holdsByRoom.getOrDefault(room.getId(), 0)))
                .filter(response -> Boolean.TRUE.equals(response.getIsAvailable()))
                .toList();
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Building building = findBuilding(request.getBuildingId());
        validateFloor(request.getFloor(), building);
        String roomNumber = request.getRoomNumber().trim();
        if (roomRepository.findByBuilding_IdAndRoomNumber(building.getId(), roomNumber).isPresent()) {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }
        Room room = roomMapper.toEntity(request, building);
        room.setRoomNumber(roomNumber);
        room.setCurrentOccupancy(0);
        room.setStatus(RoomStatus.AVAILABLE);
        return toResponseWithAvailability(roomRepository.save(room), 0);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        Building building = findBuilding(request.getBuildingId());
        validateFloor(request.getFloor(), building);
        String roomNumber = request.getRoomNumber().trim();
        if ((!room.getRoomNumber().equals(roomNumber) || !room.getBuilding().getId().equals(building.getId()))
                && roomRepository.findByBuilding_IdAndRoomNumber(building.getId(), roomNumber).isPresent()) {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }

        int heldSlots = getPendingContractCount(id);
        int occupiedAndHeld = room.getCurrentOccupancy() + heldSlots;
        if (request.getCapacity() < occupiedAndHeld) {
            throw new AppException(ErrorCode.ROOM_CAPACITY_BELOW_OCCUPANCY);
        }
        if (!room.getBuilding().getId().equals(building.getId())
                && (occupiedAndHeld > 0
                || roomAssignmentRepository.existsByRoom_Id(id))) {
                || roomAssignmentRepository.existsByRoom_Id(id)
                || dormApplicationRepository.existsByRequestedRoomId(id))) {
            throw new AppException(ErrorCode.ROOM_HAS_HISTORY);
        }

        room.setRoomNumber(roomNumber);
        room.setFloor(request.getFloor());
        room.setBuilding(building);
        room.setRoomType(request.getRoomType());
        room.setCapacity(request.getCapacity());
        room.setPricePerMonth(request.getPricePerMonth());
        room.setElectricMeterNumber(request.getElectricMeterNumber());
        room.setWaterMeterNumber(request.getWaterMeterNumber());
        room.setDescription(request.getDescription());
        refreshCapacityStatus(room, heldSlots);
        return toResponseWithAvailability(roomRepository.save(room), heldSlots);
    }

    @Transactional
    public RoomResponse updateRoomStatus(Long id, RoomStatus status) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        int heldSlots = getPendingContractCount(id);
        int occupiedAndHeld = room.getCurrentOccupancy() + heldSlots;
        if (status == RoomStatus.MAINTENANCE && occupiedAndHeld > 0) {
            throw new AppException(ErrorCode.ROOM_HAS_ACTIVE_CONTRACTS);
        }
        if (status == RoomStatus.RESERVED && occupiedAndHeld < room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                    "Chỉ được đặt trạng thái RESERVED khi toàn bộ chỗ đã có người hoặc được giữ");
        }
        if (status == RoomStatus.OCCUPIED && room.getCurrentOccupancy() < room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_NOT_AVAILABLE,
                    "Trạng thái OCCUPIED chỉ hợp lệ khi phòng đã đủ người");
        }
        if (status == RoomStatus.AVAILABLE && occupiedAndHeld >= room.getCapacity()) {
            throw new AppException(ErrorCode.ROOM_FULL);
        }
        room.setStatus(status);
        return toResponseWithAvailability(roomRepository.save(room), heldSlots);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getCurrentOccupancy() > 0
                || getPendingContractCount(id) > 0
                || roomAssignmentRepository.existsByRoom_Id(id)) {
                || roomAssignmentRepository.existsByRoom_Id(id)
                || dormApplicationRepository.existsByRequestedRoomId(id)) {
            throw new AppException(ErrorCode.ROOM_HAS_HISTORY);
        }
        roomRepository.delete(room);
    }

    private RoomResponse toResponseWithAvailability(Room room, int heldSlots) {
        RoomResponse response = roomMapper.toResponse(room);
        boolean hasSpace = room.getCurrentOccupancy() + heldSlots < room.getCapacity();
        response.setIsAvailable(room.getStatus() == RoomStatus.AVAILABLE && hasSpace);
        return response;
    }

    private Map<Long, Integer> getPendingContractCounts(List<Room> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        return contractRepository.findRoomHoldCounts(roomIds, CAPACITY_HOLD_STATUSES).stream()
                .collect(Collectors.toMap(
                        ContractRepository.RoomHoldCountProjection::getRoomId,
                        projection -> Math.toIntExact(projection.getHoldCount()),
                        Integer::sum));
    }

    private void refreshCapacityStatus(Room room, int heldSlots) {
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            return;
        }
        room.setStatus(room.getCurrentOccupancy() + heldSlots >= room.getCapacity()
                ? RoomStatus.RESERVED
                : RoomStatus.AVAILABLE);
    }

    private int getPendingContractCount(Long roomId) {
        return Math.toIntExact(contractRepository.countByRoom_IdAndStatusIn(roomId, CAPACITY_HOLD_STATUSES));
    }

    private void validateFloor(Integer floor, Building building) {
        if (floor == null || floor < 1 || floor > building.getTotalFloors()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Tầng phòng phải nằm trong số tầng của tòa nhà");
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Kích thước trang phải từ 1 đến " + MAX_PAGE_SIZE);
        }
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
    }
}
