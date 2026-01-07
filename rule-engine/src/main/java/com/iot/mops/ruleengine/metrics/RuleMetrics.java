package com.iot.mops.ruleengine.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class RuleMetrics {

    private final Counter messagesProcessed;
    private final Counter instantAlertsTriggered;
    private final Counter windowAlertsTriggered;
    private final Timer ruleEvaluationTime;
    private final MeterRegistry registry;

    public RuleMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Счётчик обработанных сообщений
        this.messagesProcessed = Counter.builder("rules.messages.processed")
                .description("Total number of messages processed by rule engine")
                .register(registry);

        // Счётчик мгновенных алертов
        this.instantAlertsTriggered = Counter.builder("rules.alerts.triggered")
                .tag("type", "instant")
                .description("Number of instant alerts triggered")
                .register(registry);

        // Счётчик window-алертов
        this.windowAlertsTriggered = Counter.builder("rules.alerts.triggered")
                .tag("type", "window")
                .description("Number of window alerts triggered")
                .register(registry);

        // Таймер оценки правил
        this.ruleEvaluationTime = Timer.builder("rules.evaluation.time")
                .description("Time taken to evaluate rules")
                .register(registry);
    }

    public void incrementMessagesProcessed() {
        messagesProcessed.increment();
    }

    public void incrementInstantAlerts() {
        instantAlertsTriggered.increment();
    }

    public void incrementWindowAlerts() {
        windowAlertsTriggered.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(ruleEvaluationTime);
    }

    public void recordAlertForDevice(String deviceId, String ruleType) {
        Counter.builder("rules.alerts.by_device")
                .tag("deviceId", deviceId)
                .tag("type", ruleType)
                .register(registry)
                .increment();
    }
}

