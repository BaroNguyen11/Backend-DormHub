package com.dormhub.exception;

import java.time.YearMonth;

public class RoomMonthlyChargeNotFoundException extends RuntimeException {

    public RoomMonthlyChargeNotFoundException(Long roomId, YearMonth billingMonth) {
        super("No meter reading for room " + roomId + " in " + billingMonth);
    }
}
