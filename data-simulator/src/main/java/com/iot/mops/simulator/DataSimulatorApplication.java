package com.iot.mops.simulator;

import com.iot.mops.common.dto.IngestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@Slf4j
public class DataSimulatorApplication implements CommandLineRunner {

    @Value("${simulator.devices:10}")
    private int devices;

    @Value("${simulator.rate:1}")
    private double ratePerSecond;

    @Value("${simulator.duration-seconds:60}")
    private long durationSeconds;

    @Value("${simulator.target-url:http://iot-controller:8080/api/ingest}")
    private String targetUrl;

    private final RestTemplate restTemplate;
    private final ThreadPoolTaskScheduler scheduler;
    private final AtomicInteger windowAlertCounter = new AtomicInteger(0);
    private static final String WINDOW_ALERT_DEVICE = "device-window-test";

    public DataSimulatorApplication() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(4);
        this.scheduler.initialize();
    }

    public static void main(String[] args) {
        SpringApplication.run(DataSimulatorApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting simulator devices={} rate={} msg/sec duration={}s target={}", devices, ratePerSecond, durationSeconds, targetUrl);
        long intervalMillis = (long) (1000 / Math.max(ratePerSecond, 0.1));

        for (int i = 0; i < devices; i++) {
            String deviceId = "device-" + i;
            scheduler.scheduleAtFixedRate(() -> sendRandom(deviceId), Duration.ofMillis(intervalMillis));
        }

        scheduler.scheduleAtFixedRate(this::sendWindowAlertBurst, Duration.ofMillis(5000));

        log.info("Window alert device '{}' will send bursts of high values to trigger window alerts", WINDOW_ALERT_DEVICE);

        scheduler.schedule(() -> {
            log.info("Simulator finished, shutting down");
            scheduler.shutdown();
            System.exit(0);
        }, Instant.now().plusSeconds(durationSeconds));
    }

    private void sendRandom(String deviceId) {
        try {
            double a = ThreadLocalRandom.current().nextDouble(0, 10);
            send(deviceId, a);
        } catch (Exception e) {
            log.warn("Failed to send from {}", deviceId, e);
        }
    }

    private void sendWindowAlertBurst() {
        int count = windowAlertCounter.incrementAndGet();

        if (count > 15) {
            windowAlertCounter.set(1);
            count = 1;
            log.info("Starting new window alert burst sequence for device '{}'", WINDOW_ALERT_DEVICE);
        }

        try {
            double a = 6.0 + ThreadLocalRandom.current().nextDouble(0, 3);
            send(WINDOW_ALERT_DEVICE, a);

            if (count == 10) {
                log.info("Window alert should trigger now for device '{}' (10 consecutive messages with a > 5)", WINDOW_ALERT_DEVICE);
            }
        } catch (Exception e) {
            log.warn("Failed to send window alert burst", e);
        }
    }

    private void send(String deviceId, double a) {
        try {
            IngestRequest request = IngestRequest.builder()
                    .deviceId(deviceId)
                    .ts(Instant.now())
                    .payload(Map.of("a", a))
                    .build();
            restTemplate.postForEntity(targetUrl, request, Void.class);
            log.debug("Sent {} -> a={:.2f}", deviceId, a);
        } catch (Exception e) {
            log.warn("Failed to send from {}: {}", deviceId, e.getMessage());
        }
    }
}
