package com.dormhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SemanticRoomSearchAnalysisResponse {
    private List<SemanticRoomSearchResponse> rooms;
    private int totalMatches;
    private int displayedCount;
    private RoomSearchStatisticsResponse statistics;
}
