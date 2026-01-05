package com.iot.mops.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class IngestRequest {

    @NotBlank
    private String deviceId;

    @NotNull
    private Instant ts;

    @NotNull
    private Map<String, Object> payload;
}
