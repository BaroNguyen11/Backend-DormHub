package com.dormhub.service;

import com.dormhub.dto.response.RoomMonthlyChargeResult;

public interface RoomMonthlyChargeService {

    RoomMonthlyChargeResult calculate(Long roomId, String month);
}
