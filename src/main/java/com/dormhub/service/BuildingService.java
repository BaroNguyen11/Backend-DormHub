package com.dormhub.service;

import com.dormhub.dto.request.BuildingRequest;
import com.dormhub.dto.response.BuildingResponse;
import com.dormhub.entity.Building;
import com.dormhub.repository.BuildingRepository;
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

    public List<BuildingResponse> getAll() {
        return buildingRepository.findAll().stream()
                .map(BuildingResponse::from)
                .toList();
    }

    public BuildingResponse getById(Long id) {
        return BuildingResponse.from(findBuilding(id));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        Building building = new Building();
        apply(building, request);
        return BuildingResponse.from(buildingRepository.save(building));
    }

    @Transactional
    public BuildingResponse update(Long id, BuildingRequest request) {
        Building building = buildingRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà"));
        apply(building, request);
        return BuildingResponse.from(buildingRepository.save(building));
    }

    @Transactional
    public void delete(Long id) {
        Building building = findBuilding(id);
        if (roomRepository.existsByBuildingId(id)) {
            throw new RuntimeException("Không thể xóa tòa nhà đang có phòng");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tòa nhà"));
    }
}
