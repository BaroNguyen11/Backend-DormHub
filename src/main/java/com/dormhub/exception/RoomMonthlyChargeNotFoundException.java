package com.dormhub.exception;

import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoomMonthlyChargeNotFoundException extends RuntimeException {

    public RoomMonthlyChargeNotFoundException(Long roomId, YearMonth billingMonth) {
        super("No meter reading for room " + roomId + " in " + billingMonth);
    }
}
