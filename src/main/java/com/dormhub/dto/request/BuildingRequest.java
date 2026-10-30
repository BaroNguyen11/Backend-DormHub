package com.dormhub.dto.request;

import com.dormhub.entity.enums.BuildingGenderPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BuildingRequest {

    @NotBlank(message = "buildingName không được để trống")
    @Size(max = 100, message = "buildingName tối đa 100 ký tự")
    private String buildingName;

    @Size(max = 255, message = "address tối đa 255 ký tự")
    private String address;

    @NotNull(message = "genderPolicy phải là MALE_ONLY hoặc FEMALE_ONLY")
    private BuildingGenderPolicy genderPolicy;

    private String description;
}
