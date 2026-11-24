package com.dormhub.dto.response;

import com.dormhub.config.ContractLandlordProperties;
import lombok.Getter;

@Getter
public class ContractLandlordResponse {
    private final String name;
    private final String representative;
    private final String position;
    private final String address;
    private final String phone;
    private final String email;

    public ContractLandlordResponse(ContractLandlordProperties properties) {
        this.name = properties.getName();
        this.representative = properties.getRepresentative();
        this.position = properties.getPosition();
        this.address = properties.getAddress();
        this.phone = properties.getPhone();
        this.email = properties.getEmail();
    }
}
