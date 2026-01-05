package com.iot.mops.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {
    @Id
    private String id;
    private String ruleId;
    private String deviceId;
    private String kind;
    private int windowSize;
    private String condition;
    private Instant triggeredAt;
    private Map<String, Object> payloadSnapshot;
    private String correlationId;
}
