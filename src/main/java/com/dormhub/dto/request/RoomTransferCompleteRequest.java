package com.dormhub.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomTransferCompleteRequest {
    private Boolean lateCompletionConfirmed;
    @Size(max = 1000)
    private String note;
}
