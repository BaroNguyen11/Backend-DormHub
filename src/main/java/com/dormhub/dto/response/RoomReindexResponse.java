package com.dormhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RoomReindexResponse {
    private int indexedRooms;
    private int removedOrphans;
}
