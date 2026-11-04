package com.dormhub.service;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.BuildingMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingService {
    
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final BuildingMapper buildingMapper;
    
    @Transactional(readOnly = true)
    public List<BuildingResponse> getAllBuildings() {
        List<Building> buildings = buildingRepository.findAll();
        return buildings.stream()
                .map(this::toBuildingResponseWithStats)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public BuildingResponse getBuildingById(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        return toBuildingResponseWithStats(building);
    }
    
    @Transactional
    public BuildingResponse createBuilding(BuildingRequest request) {
        if (buildingRepository.existsByBuildingCode(request.getBuildingCode())) {
            throw new AppException(ErrorCode.BUILDING_CODE_EXISTS);
        }
        
        Building building = buildingMapper.toEntity(request);
        Building saved = buildingRepository.save(building);
        return toBuildingResponseWithStats(saved);
    }
    
    @Transactional
    public BuildingResponse updateBuilding(Long id, BuildingRequest request) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        
        // Check if buildingCode changed and conflicts
        if (!building.getBuildingCode().equals(request.getBuildingCode()) 
                && buildingRepository.existsByBuildingCode(request.getBuildingCode())) {
            throw new AppException(ErrorCode.BUILDING_CODE_EXISTS);
        }
        
        building.setBuildingCode(request.getBuildingCode());
        building.setName(request.getName());
        building.setAddress(request.getAddress());
        building.setTotalFloors(request.getTotalFloors());
        building.setGenderRestriction(request.getGenderRestriction());
        building.setDescription(request.getDescription());
        
        Building updated = buildingRepository.save(building);
        return toBuildingResponseWithStats(updated);
    }
    
    @Transactional
    public void deleteBuilding(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        
        // Check if building has rooms
        long roomCount = roomRepository.countByBuilding_Id(id);
        if (roomCount > 0) {
            throw new AppException(ErrorCode.BUILDING_HAS_ROOMS, 
                    "Cannot delete building with existing rooms");
        }
        
        buildingRepository.delete(building);
    }
    
    private BuildingResponse toBuildingResponseWithStats(Building building) {
        long totalRooms = roomRepository.countByBuilding_Id(building.getId());
        long availableRooms = roomRepository.countByBuilding_IdAndStatus(
                building.getId(), RoomStatus.AVAILABLE);
        
        return buildingMapper.toResponse(building, totalRooms, availableRooms);
    }
}
