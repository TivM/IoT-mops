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
@Document(collection = "iot_messages")
public class IotMessage {
    @Id
    private String id;
    private String deviceId;
    private Instant ts;
    private Map<String, Object> payload;
    private Instant ingestedAt;
    private String correlationId;
}
