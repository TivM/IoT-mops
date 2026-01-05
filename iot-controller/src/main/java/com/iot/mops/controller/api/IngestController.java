package com.iot.mops.controller.api;

import com.iot.mops.common.dto.IngestRequest;
import com.iot.mops.controller.service.IngestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class IngestController {

    private final IngestService ingestService;

    @PostMapping("/ingest")
    public ResponseEntity<Void> ingest(@Valid @RequestBody IngestRequest request) {
        ingestService.ingest(request);
        return ResponseEntity.accepted().build();
    }
}
