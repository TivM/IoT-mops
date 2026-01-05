package com.iot.mops.controller.service;

import com.iot.mops.common.dto.IngestRequest;
import com.iot.mops.common.dto.IotMessage;
import com.iot.mops.common.dto.QueueEnvelope;
import com.iot.mops.controller.store.IotMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestService {

    private final IotMessageRepository repository;
    private final RabbitTemplate rabbitTemplate;

    // TODO: move to config
    private static final String EXCHANGE = "iot.events";
    private static final String ROUTING_KEY_TEMPLATE = "device.%s";

    public void ingest(IngestRequest request) {
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
    }
}
