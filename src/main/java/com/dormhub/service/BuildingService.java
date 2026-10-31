package com.dormhub.service;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.entity.Building;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.BuildingMapper;
import com.dormhub.entity.enums.DormApplicationStatus;
import com.dormhub.entity.enums.RoomAssignmentStatus;
import com.dormhub.repository.RoomAssignmentRepository;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.DormApplicationRepository;
import com.dormhub.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final DormApplicationRepository dormApplicationRepository;
    private final BuildingMapper buildingMapper;

    public List<BuildingResponse> getAll() {
        return buildingRepository.findAll().stream()
                .map(buildingMapper::toResponse)
                .toList();
    }

    public BuildingResponse getById(Long id) {
        return buildingMapper.toResponse(findBuilding(id));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        Building building = new Building();
        apply(building, request);
        return buildingMapper.toResponse(buildingRepository.save(building));
    }

    @Transactional
    public BuildingResponse update(Long id, BuildingRequest request) {
        Building building = buildingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        if (building.getGenderPolicy() != request.getGenderPolicy()
                && (roomAssignmentRepository.existsByRoomBuildingIdAndStatus(
                            id, RoomAssignmentStatus.ASSIGNED)
                    || dormApplicationRepository.existsByRequestedRoomBuildingIdAndStatusIn(
                            id, List.of(DormApplicationStatus.PENDING, DormApplicationStatus.APPROVED)))) {
            throw new AppException(ErrorCode.BUILDING_GENDER_POLICY_IN_USE);
        }
        apply(building, request);
        return buildingMapper.toResponse(buildingRepository.save(building));
    }

    @Transactional
    public void delete(Long id) {
        Building building = findBuilding(id);
        if (roomRepository.existsByBuildingId(id)) {
            throw new AppException(ErrorCode.BUILDING_HAS_ROOMS);
        }
        buildingRepository.delete(building);
    }

    private void apply(Building building, BuildingRequest request) {
        building.setBuildingName(request.getBuildingName().trim());
        building.setAddress(request.getAddress());
        building.setGenderPolicy(request.getGenderPolicy());
        building.setDescription(request.getDescription());
    }

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
    }
}
