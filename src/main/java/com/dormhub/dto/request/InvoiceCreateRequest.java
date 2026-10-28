package com.dormhub.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Getter
@Setter
@NoArgsConstructor
public class InvoiceCreateRequest {

    @NotBlank
    @Pattern(regexp = "(?i)\\s*(ROOM_FEE|UTILITY|DEPOSIT)\\s*")
    private String invoiceType;

    @Positive
    private Long roomId;

    @Positive
    private Long contractId;

    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])")
    private String billingMonth;

    @JsonIgnore
    @AssertTrue(message = "ROOM_FEE requires contractId and billingMonth; UTILITY requires roomId and billingMonth")
    public boolean isValidOwner() {
        if (invoiceType == null) {
            return true;
        }

        return switch (invoiceType.trim().toUpperCase(Locale.ROOT)) {
            case "ROOM_FEE" -> contractId != null && roomId == null && billingMonth != null;
            case "UTILITY" -> roomId != null && contractId == null && billingMonth != null;
            case "DEPOSIT" -> true;
            default -> true;
        };
    }
}
