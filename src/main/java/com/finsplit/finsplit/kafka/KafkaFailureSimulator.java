package com.finsplit.finsplit.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class KafkaFailureSimulator {

    // Toggle via REST endpoint to simulate failures
    private final AtomicBoolean failureEnabled = new AtomicBoolean(false);
    private final AtomicInteger failureCount   = new AtomicInteger(0);

    public void enableFailure() {
        failureEnabled.set(true);
        failureCount.set(0);
        log.warn("⚠️  Kafka failure simulation ENABLED");
    }

    public void disableFailure() {
        failureEnabled.set(false);
        failureCount.set(0);
        log.info("✅ Kafka failure simulation DISABLED");
    }

    public boolean isFailureEnabled() {
        return failureEnabled.get();
    }

    // Throws on first N attempts — simulates transient failure
    public void maybeThrow(String expenseId) {
        if (!failureEnabled.get()) return;

        int count = failureCount.incrementAndGet();
        log.warn("🔥 Simulating failure #{} for expenseId={}", count, expenseId);
        throw new RuntimeException(
                "Simulated consumer failure #" + count +
                        " for expenseId=" + expenseId);
    }
}
