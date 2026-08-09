package com.finsplit.finsplit.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventProducer {

    private final KafkaTemplate<String, ExpenseAddedEvent> kafkaTemplate;

    @Value("${app.kafka.topic.expense-added}")
    private String topic;

    public void publishExpenseAdded(ExpenseAddedEvent event) {
        // Use groupId as the key — ensures all events for a group
        // go to the same partition (ordering guarantee per group)
        CompletableFuture<SendResult<String, ExpenseAddedEvent>> future =
                kafkaTemplate.send(topic, event.getGroupId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish ExpenseAddedEvent for expense {}: {}",
                        event.getExpenseId(), ex.getMessage());
            } else {
                log.info("Published ExpenseAddedEvent → topic={} partition={} offset={} expenseId={}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.getExpenseId());
            }
        });
    }
}
