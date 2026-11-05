package com.dormhub.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyOccupancyResponse {

    private final int month;
    private final Long occupiedResidents;
    private final Long capacity;
    private final BigDecimal occupancyRate;
}
