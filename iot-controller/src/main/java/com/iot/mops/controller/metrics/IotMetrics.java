package com.iot.mops.controller.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class IotMetrics {

    private final Counter messagesReceived;
    private final Counter messagesProcessed;
    private final Counter validationErrors;
    private final Timer processingTime;
    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, AtomicInteger> deviceMessageCounts = new ConcurrentHashMap<>();

    public IotMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // Счётчик полученных сообщений
        this.messagesReceived = Counter.builder("iot.messages.received")
                .description("Total number of IoT messages received")
                .register(registry);

        // Счётчик успешно обработанных сообщений
        this.messagesProcessed = Counter.builder("iot.messages.processed")
                .description("Total number of IoT messages successfully processed")
                .register(registry);

        // Счётчик ошибок валидации
        this.validationErrors = Counter.builder("iot.validation.errors")
                .description("Total number of validation errors")
                .register(registry);

        // Таймер обработки сообщений
        this.processingTime = Timer.builder("iot.processing.time")
                .description("Time taken to process IoT messages")
                .register(registry);

        // Gauge для количества активных устройств
        registry.gaugeMapSize("iot.devices.active", null, deviceMessageCounts);
    }

    public void incrementMessagesReceived() {
        messagesReceived.increment();
    }

    public void incrementMessagesProcessed() {
        messagesProcessed.increment();
    }

    public void incrementValidationErrors() {
        validationErrors.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(processingTime);
    }

    public void recordDeviceMessage(String deviceId) {
        deviceMessageCounts.computeIfAbsent(deviceId, k -> {
            AtomicInteger count = new AtomicInteger(0);
            // Создаём gauge для каждого устройства
            registry.gauge("iot.device.messages", 
                    io.micrometer.core.instrument.Tags.of("deviceId", deviceId), 
                    count);
            return count;
        }).incrementAndGet();
    }
}

