package com.dormhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dormhub.contract.landlord")
public class ContractLandlordProperties {
    private String name;
    private String representative;
    private String position;
    private String address;
    private String phone;
    private String email;
}
