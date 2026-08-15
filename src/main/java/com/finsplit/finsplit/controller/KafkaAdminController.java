package com.finsplit.finsplit.controller;

import com.finsplit.finsplit.kafka.KafkaFailureSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kafka-admin")
@RequiredArgsConstructor
public class KafkaAdminController {

    private final KafkaFailureSimulator failureSimulator;

    // Enable failure simulation
    @PostMapping("/simulate-failure/enable")
    public ResponseEntity<Map<String, Object>> enableFailure() {
        failureSimulator.enableFailure();
        return ResponseEntity.ok(Map.of(
                "status",  "enabled",
                "message", "Next consumed events will throw RuntimeException",
                "effect",  "Kafka will retry 3 times then send to DLT"
        ));
    }

    // Disable failure simulation
    @PostMapping("/simulate-failure/disable")
    public ResponseEntity<Map<String, Object>> disableFailure() {
        failureSimulator.disableFailure();
        return ResponseEntity.ok(Map.of(
                "status",  "disabled",
                "message", "Consumer back to normal operation"
        ));
    }

    // Check current state
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "failureSimulationEnabled", failureSimulator.isFailureEnabled(),
                "topic",                    "expense-added",
                "dltTopic",                 "expense-added-dlt",
                "retryAttempts",            3
        ));
    }
}
