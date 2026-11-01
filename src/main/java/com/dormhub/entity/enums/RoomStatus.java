package com.dormhub.entity.enums;

/** Room lifecycle and availability states persisted by the PostgreSQL room_status enum. */
public enum RoomStatus {
    AVAILABLE,
    FULL,
    MAINTENANCE,
    INACTIVE
}
