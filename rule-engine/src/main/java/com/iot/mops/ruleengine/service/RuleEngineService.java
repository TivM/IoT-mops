package com.iot.mops.ruleengine.service;

import com.iot.mops.common.dto.Alert;
import com.iot.mops.common.dto.QueueEnvelope;
import com.iot.mops.ruleengine.store.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final AlertRepository alertRepository;

    @Value("${app.rules.window-size:10}")
    private int windowSize;

    @Value("${app.rules.max-window-age-sec:60}")
    private long maxWindowAgeSec;

    private final Map<String, Deque<QueueEnvelope>> windows = new ConcurrentHashMap<>();

    @RabbitListener(queues = "${app.rabbit.queue}")
    public void handle(QueueEnvelope envelope) {
        log.debug("Received envelope {}", envelope);

        checkInstantRule(envelope);
        checkWindowRule(envelope);
    }

    private void checkInstantRule(QueueEnvelope envelope) {
        Double aValue = extractA(envelope.getPayload());
        if (aValue != null && aValue > 5) {
            saveAlert("instant-a-gt-5", "instant", 1, envelope);
        }
    }

    private void checkWindowRule(QueueEnvelope envelope) {
        Deque<QueueEnvelope> deque = windows.computeIfAbsent(envelope.getDeviceId(), k -> new ArrayDeque<>());
        deque.addLast(envelope);
        while (deque.size() > windowSize) {
            deque.pollFirst();
        }
        evictOld(envelope.getDeviceId(), deque);

        if (deque.size() == windowSize && deque.stream().allMatch(q -> {
            Double a = extractA(q.getPayload());
            return a != null && a > 5;
        })) {
            saveAlert("window-a-gt-5-n-" + windowSize, "window", deque.size(), envelope);
        }
    }

    @Scheduled(fixedDelayString = "60000")
    public void cleanup() {
        windows.forEach(this::evictOld);
    }

    private void evictOld(String deviceId, Deque<QueueEnvelope> deque) {
        Instant cutoff = Instant.now().minus(Duration.ofSeconds(maxWindowAgeSec));
        while (!deque.isEmpty() && deque.peekFirst().getTs().isBefore(cutoff)) {
            deque.pollFirst();
        }
        if (deque.isEmpty()) {
            windows.remove(deviceId);
        }
    }

    private void saveAlert(String ruleId, String kind, int size, QueueEnvelope envelope) {
        Alert alert = Alert.builder()
                .ruleId(ruleId)
                .deviceId(envelope.getDeviceId())
                .kind(kind)
                .windowSize(size)
                .condition("payload.a > 5")
                .triggeredAt(Instant.now())
                .payloadSnapshot(envelope.getPayload())
                .correlationId(envelope.getCorrelationId())
                .build();
        alertRepository.save(alert);
        log.info("Alert triggered {}", alert);
    }

    private Double extractA(Map<String, Object> payload) {
        Object value = payload.get("a");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
