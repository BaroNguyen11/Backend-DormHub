package com.dormhub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {
    private List<Map<String, Object>> messages;
    private String message;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}

