package com.dormhub.service;

import com.dormhub.dto.response.RoomMonthlyChargeResult;
import java.time.YearMonth;

public interface RoomMonthlyChargeService {

    RoomMonthlyChargeResult calculate(Long roomId, YearMonth billingMonth);
}
