package com.dormhub.service;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.User;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomAssignmentStatus;
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
import com.dormhub.repository.StudentRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final List<ContractStatus> CAPACITY_HOLD_STATUSES = List.of(ContractStatus.PENDING);

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;
    private final DormApplicationRepository dormApplicationRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final RoomMapper roomMapper;
    private final RoomStatusService roomStatusService;

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
        Map<Long, Integer> holdsByRoom = getCapacityReservations(rooms.getContent());
        return rooms.map(room -> toResponseWithAvailability(
                room, holdsByRoom.getOrDefault(room.getId(), 0)));
    }

    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        RoomResponse response = toResponseWithAvailability(room, getRoomCapacityReservations(room.getId()));
        response.setResidents(roomAssignmentRepository.findCurrentResidentsByRoom(
                id, List.of(RoomAssignmentStatus.ASSIGNED, RoomAssignmentStatus.CHECKED_IN), ContractStatus.ACTIVE));
        return response;
    }

    public RoomResponse getRoomByIdForStudent(Long id, User user) {
        Gender gender = getStudentGender(user);
        if (gender == null) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
        Room room = roomRepository.findDetailById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getBuilding() == null || room.getBuilding().getGenderRestriction() != gender) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
        return toResponseWithAvailability(room, getRoomCapacityReservations(room.getId()));
    }

    public List<RoomResponse> getAvailableRooms() {
        List<Room> rooms = roomRepository.findAvailableWithPhysicalSpace(RoomStatus.AVAILABLE);
        Map<Long, Integer> holdsByRoom = getCapacityReservations(rooms);
        return rooms.stream()
                .map(room -> toResponseWithAvailability(room, holdsByRoom.getOrDefault(room.getId(), 0)))
                .filter(response -> Boolean.TRUE.equals(response.getIsAvailable()))
                .toList();
    }

    /**
     * P1: danh sách phòng trống cho SINH VIÊN để tự chọn phòng khi nộp đơn đăng ký.
     * Lọc theo giới tính của tòa (khớp giới tính SV) và còn chỗ trống thật.
     */
    public List<RoomResponse> getAvailableRoomsForStudent(User user) {
        Gender studentGender = getStudentGender(user);
        if (studentGender == null) {
            return List.of();
        }
        List<Room> rooms = roomRepository.findAvailableWithPhysicalSpaceAndGender(
                RoomStatus.AVAILABLE, studentGender);
        Map<Long, Integer> holdsByRoom = getCapacityReservations(rooms);
        return rooms.stream()
                .filter(room -> room.getBuilding() != null
                        && room.getBuilding().getGenderRestriction() == studentGender)
                .map(room -> toResponseWithAvailability(room, holdsByRoom.getOrDefault(room.getId(), 0)))
                .filter(response -> Boolean.TRUE.equals(response.getIsAvailable()))
                .toList();
    }

    private Gender getStudentGender(User user) {
        Gender gender = studentRepository.findByUserId(user.getId())
                .map(student -> student.getGender())
                .orElse(null);
        return gender == Gender.MALE || gender == Gender.FEMALE ? gender : null;
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

        int heldSlots = getRoomCapacityReservations(id);
        int occupiedAndHeld = room.getCurrentOccupancy() + heldSlots;
        if (request.getCapacity() < occupiedAndHeld) {
            throw new AppException(ErrorCode.ROOM_CAPACITY_BELOW_OCCUPANCY);
        }
        if (!room.getBuilding().getId().equals(building.getId())
                && (occupiedAndHeld > 0
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
        // Tính lại trạng thái qua hàm chuẩn duy nhất (phân biệt RESERVED/OCCUPIED theo hold)
        roomStatusService.refreshStatus(room, false);
        return toResponseWithAvailability(roomRepository.save(room), heldSlots);
    }

    @Transactional
    public RoomResponse updateRoomStatus(Long id, RoomStatus status) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
        int heldSlots = getRoomCapacityReservations(id);
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
                || getRoomCapacityReservations(id) > 0
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
        response.setAvailableSlots(Math.max(0, room.getCapacity() - room.getCurrentOccupancy() - heldSlots));
        return response;
    }

    private Map<Long, Integer> getCapacityReservations(List<Room> rooms) {
        if (rooms.isEmpty()) return Map.of();
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();
        Map<Long, Integer> reservations = new HashMap<>(getPendingContractCounts(rooms));
        dormApplicationRepository.findRoomApplicationHoldCounts(
                        roomIds, com.dormhub.entity.enums.DormApplicationStatus.PENDING)
                .forEach(hold -> reservations.merge(hold.getRoomId(), Math.toIntExact(hold.getHoldCount()), Integer::sum));
        return reservations;
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

    private int getRoomCapacityReservations(Long roomId) {
        int pendingContracts = Math.toIntExact(
                contractRepository.countByRoom_IdAndStatusIn(roomId, CAPACITY_HOLD_STATUSES));
        int pendingApplications = Math.toIntExact(dormApplicationRepository.countByRequestedRoomIdAndStatusIn(
                roomId, List.of(com.dormhub.entity.enums.DormApplicationStatus.PENDING)));
        return pendingContracts + pendingApplications;
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

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
    }
}
