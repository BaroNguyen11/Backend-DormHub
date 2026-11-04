package com.dormhub.dto.request;

import com.dormhub.entity.enums.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingRequest {
    @NotBlank(message = "Building code is required")
    @Size(max = 20)
    private String buildingCode;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;
    
    @Size(max = 255)
    private String address;
    
    @NotNull(message = "Total floors is required")
    @Min(1)
    private Integer totalFloors;
    
    private Gender genderRestriction;
    
    private String description;
}
