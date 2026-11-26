package com.dormhub.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DormApplicationRequest {

    @Positive(message = "roomId phải lớn hơn 0")
    private Long roomId;

    @Size(max = 2000, message = "note tối đa 2000 ký tự")
    private String note;

    private Boolean contractAccepted;
}
