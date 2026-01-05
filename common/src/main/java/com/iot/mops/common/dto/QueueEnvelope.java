package com.iot.mops.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEnvelope {
    private String deviceId;
    private Instant ts;
    private Map<String, Object> payload;
    private Instant ingestedAt;
    private String correlationId;
}
