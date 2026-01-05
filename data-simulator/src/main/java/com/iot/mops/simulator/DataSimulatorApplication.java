package com.iot.mops.simulator;

import com.iot.mops.common.dto.IngestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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

    public DataSimulatorApplication(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .additionalCustomizers(rt -> {
                    if (rt.getRequestFactory() instanceof SimpleClientHttpRequestFactory factory) {
                        factory.setConnectTimeout(2000);
                        factory.setReadTimeout(2000);
                    }
                })
                .build();
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
        Instant end = Instant.now().plusSeconds(durationSeconds);

        for (int i = 0; i < devices; i++) {
            String deviceId = "device-" + i;
            scheduler.scheduleAtFixedRate(() -> sendOnce(deviceId), intervalMillis);
        }

        scheduler.schedule(() -> {
            log.info("Simulator finished, shutting down");
            scheduler.shutdown();
            System.exit(0);
        }, Instant.now().plusSeconds(durationSeconds));
    }

    private void sendOnce(String deviceId) {
        try {
            double a = ThreadLocalRandom.current().nextDouble(0, 10);
            IngestRequest request = IngestRequest.builder()
                    .deviceId(deviceId)
                    .ts(Instant.now())
                    .payload(Map.of("a", a))
                    .build();
            restTemplate.postForEntity(targetUrl, request, Void.class);
            log.debug("Sent {} -> a={}", deviceId, a);
        } catch (Exception e) {
            log.warn("Failed to send from {}", deviceId, e);
        }
    }
}
