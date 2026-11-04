package com.dormhub.service;

import com.dormhub.dto.request.RoomRequest;
import com.dormhub.dto.response.RoomResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.Room;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.entity.enums.RoomType;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.RoomMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final RoomMapper roomMapper;
    
    @Transactional(readOnly = true)
    public Page<RoomResponse> getAllRooms(RoomStatus status, RoomType roomType, Long buildingId, Pageable pageable) {
        Page<Room> rooms;
        
        if (status != null && buildingId != null) {
            rooms = roomRepository.findByStatusAndBuilding_Id(status, buildingId, pageable);
        } else if (status != null) {
            rooms = roomRepository.findByStatus(status, pageable);
        } else if (roomType != null) {
            rooms = roomRepository.findByRoomType(roomType, pageable);
        } else if (buildingId != null) {
            rooms = roomRepository.findByBuilding_Id(buildingId, pageable);
        } else {
            rooms = roomRepository.findAll(pageable);
        }
        
        return rooms.map(roomMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = findRoomById(id);
        return roomMapper.toResponse(room);
    }
    
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms() {
        List<Room> rooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        return rooms.stream()
                .filter(room -> room.getCurrentOccupancy() < room.getCapacity())
                .map(roomMapper::toResponse)
                .toList();
    }
    
    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        
        // Check duplicate room number in same building
        if (roomRepository.findByBuilding_IdAndRoomNumber(
                request.getBuildingId(), request.getRoomNumber()).isPresent()) {
            throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
        }
        
        Room room = roomMapper.toEntity(request, building);
        Room saved = roomRepository.save(room);
        return roomMapper.toResponse(saved);
    }
    
    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findRoomById(id);
        
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        
        // Check duplicate if room number or building changed
        if (!room.getRoomNumber().equals(request.getRoomNumber()) 
                || !room.getBuilding().getId().equals(request.getBuildingId())) {
            if (roomRepository.findByBuilding_IdAndRoomNumber(
                    request.getBuildingId(), request.getRoomNumber()).isPresent()) {
                throw new AppException(ErrorCode.ROOM_ALREADY_EXISTS);
            }
        }
        
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setBuilding(building);
        room.setRoomType(request.getRoomType());
        room.setCapacity(request.getCapacity());
        room.setPricePerMonth(request.getPricePerMonth());
        room.setElectricMeterNumber(request.getElectricMeterNumber());
        room.setWaterMeterNumber(request.getWaterMeterNumber());
        room.setDescription(request.getDescription());
        
        // Auto-update status based on occupancy
        if (room.getCurrentOccupancy() >= room.getCapacity()) {
            room.setStatus(RoomStatus.OCCUPIED);
        } else if (room.getStatus() == RoomStatus.OCCUPIED) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        
        Room updated = roomRepository.save(room);
        return roomMapper.toResponse(updated);
    }
    
    @Transactional
    public RoomResponse updateRoomStatus(Long id, RoomStatus status) {
        Room room = findRoomById(id);
        room.setStatus(status);
        Room updated = roomRepository.save(room);
        return roomMapper.toResponse(updated);
    }
    
    @Transactional
    public void deleteRoom(Long id) {
        Room room = findRoomById(id);
        
        // Check if room is occupied
        if (room.getCurrentOccupancy() > 0) {
            throw new AppException(ErrorCode.ROOM_OCCUPIED, 
                    "Cannot delete room with current occupants");
        }
        
        roomRepository.delete(room);
    }
    
    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }
}
