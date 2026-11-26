package com.dormhub.dto.request;

import com.dormhub.entity.enums.DepositStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutCompleteRequest {

    private DepositStatus depositDisposition;

    @jakarta.validation.constraints.Size(max = 1000)
    private String note;
}
