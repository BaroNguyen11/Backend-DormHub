package com.dormhub.service;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.entity.Building;
import com.dormhub.entity.enums.ContractStatus;
import com.dormhub.entity.enums.Gender;
import com.dormhub.entity.enums.RoomStatus;
import com.dormhub.exception.AppException;
import com.dormhub.exception.ErrorCode;
import com.dormhub.mapper.BuildingMapper;
import com.dormhub.repository.BuildingRepository;
import com.dormhub.repository.ContractRepository;
import com.dormhub.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuildingService {

    private static final List<ContractStatus> CURRENT_CONTRACT_STATUSES =
            List.of(ContractStatus.PENDING, ContractStatus.ACTIVE);

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final BuildingMapper buildingMapper;

    public List<BuildingResponse> getAllBuildings() {
        return buildingRepository.findAll().stream().map(this::toResponseWithStats).toList();
    }

    public BuildingResponse getBuildingById(Long id) {
        return toResponseWithStats(findBuilding(id));
    }

    @Transactional
    public BuildingResponse createBuilding(BuildingRequest request) {
        validateGender(request.getGenderRestriction());
        String code = request.getBuildingCode().trim();
        if (buildingRepository.existsByBuildingCode(code)) {
            throw new AppException(ErrorCode.BUILDING_CODE_EXISTS);
        }
        Building building = buildingMapper.toEntity(request);
        building.setBuildingCode(code);
        building.setName(request.getName().trim());
        return toResponseWithStats(buildingRepository.save(building));
    }

    @Transactional
    public BuildingResponse updateBuilding(Long id, BuildingRequest request) {
        validateGender(request.getGenderRestriction());
        Building building = buildingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
        String code = request.getBuildingCode().trim();
        if (!building.getBuildingCode().equals(code) && buildingRepository.existsByBuildingCode(code)) {
            throw new AppException(ErrorCode.BUILDING_CODE_EXISTS);
        }

        Gender currentGender = building.getGenderRestriction();
        Gender requestedGender = request.getGenderRestriction();
        if (currentGender != requestedGender
                && contractRepository.existsCurrentContractWithDifferentGender(
                        id, CURRENT_CONTRACT_STATUSES, requestedGender)
                && !request.isConfirmGenderPolicyChange()) {
            throw new AppException(ErrorCode.BUILDING_GENDER_POLICY_CONFIRMATION_REQUIRED);
        }

        building.setBuildingCode(code);
        building.setName(request.getName().trim());
        building.setAddress(request.getAddress());
        building.setTotalFloors(request.getTotalFloors());
        building.setGenderRestriction(requestedGender);
        building.setDescription(request.getDescription());
        return toResponseWithStats(buildingRepository.save(building));
    }

    @Transactional
    public void deleteBuilding(Long id) {
        Building building = findBuilding(id);
        if (roomRepository.countByBuilding_Id(id) > 0) {
            throw new AppException(ErrorCode.BUILDING_HAS_ROOMS);
        }
        buildingRepository.delete(building);
    }

    private Building findBuilding(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BUILDING_NOT_FOUND));
    }

    private void validateGender(Gender gender) {
        if (gender != Gender.MALE && gender != Gender.FEMALE) {
            throw new AppException(ErrorCode.ROOM_GENDER_POLICY_REQUIRED);
        }
    }

    private BuildingResponse toResponseWithStats(Building building) {
        long totalRooms = roomRepository.countByBuilding_Id(building.getId());
        long availableRooms = roomRepository.countByBuilding_IdAndStatus(
                building.getId(), RoomStatus.AVAILABLE);
        return buildingMapper.toResponse(building, totalRooms, availableRooms);
    }
}
