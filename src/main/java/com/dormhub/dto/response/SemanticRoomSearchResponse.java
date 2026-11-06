package com.dormhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SemanticRoomSearchResponse {
    private RoomResponse room;
    private Double similarityScore;
}
