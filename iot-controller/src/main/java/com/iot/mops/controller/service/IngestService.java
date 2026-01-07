package com.iot.mops.controller.service;

import com.iot.mops.common.dto.IngestRequest;
import com.iot.mops.common.dto.IotMessage;
import com.iot.mops.common.dto.QueueEnvelope;
import com.iot.mops.controller.metrics.IotMetrics;
import com.iot.mops.controller.store.IotMessageRepository;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestService {

    private final IotMessageRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final IotMetrics metrics;

    private static final String EXCHANGE = "iot.events";
    private static final String ROUTING_KEY_TEMPLATE = "device.%s";

    public void ingest(IngestRequest request) {
        metrics.incrementMessagesReceived();
        Timer.Sample timer = metrics.startTimer();
        
        try {
            String correlationId = UUID.randomUUID().toString();
            Instant ingestedAt = Instant.now();

            IotMessage message = IotMessage.builder()
                    .deviceId(request.getDeviceId())
                    .ts(request.getTs())
                    .payload(request.getPayload())
                    .ingestedAt(ingestedAt)
                    .correlationId(correlationId)
                    .build();

            repository.save(message);

            QueueEnvelope envelope = QueueEnvelope.builder()
                    .deviceId(request.getDeviceId())
                    .ts(request.getTs())
                    .payload(request.getPayload())
                    .ingestedAt(ingestedAt)
                    .correlationId(correlationId)
                    .build();

            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_TEMPLATE.formatted(request.getDeviceId()), envelope);
            
            metrics.incrementMessagesProcessed();
            metrics.recordDeviceMessage(request.getDeviceId());
            
            log.debug("Processed message from {} with correlationId {}", request.getDeviceId(), correlationId);
        } finally {
            metrics.stopTimer(timer);
        }
    }
}
