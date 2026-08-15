//package com.finsplit.finsplit.kafka;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.annotation.RetryableTopic;
//import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
//import org.springframework.retry.annotation.Backoff;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ExpenseEventConsumer {
//
//    private final BalanceNotificationService notificationService;
//
//    @KafkaListener(
//            topics        = "${app.kafka.topic.expense-added}",
//            groupId       = "${spring.kafka.consumer.group-id}",
//            containerFactory = "kafkaListenerContainerFactory"
//    )
//    public void consume(ConsumerRecord<String, ExpenseAddedEvent> record) {
//        ExpenseAddedEvent event = record.value();
//
//        log.info("──────────────────────────────────────────────");
//        log.info("Consumed event from topic={} partition={} offset={}",
//                record.topic(), record.partition(), record.offset());
//        log.info("EventId={} GroupId={} ExpenseId={}",
//                event.getEventId(), event.getGroupId(), event.getExpenseId());
//
//        notificationService.recalculateAndNotify(event);
//    }
//}

package com.finsplit.finsplit.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventConsumer {

    private final BalanceNotificationService notificationService;
    private final KafkaFailureSimulator      failureSimulator;

    // ── Main consumer with retry ──────────────────────────
    // @RetryableTopic handles retry automatically:
    // - attempts=3 means 1 original + 2 retries
    // - creates expense-added-retry-0, expense-added-retry-1 topics
    // - after all retries exhausted, sends to expense-added-dlt
    @RetryableTopic(
            attempts         = "3",
            backoff          = @Backoff(delay = 2000, multiplier = 2.0),
            topicSuffixingStrategy =
                    TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix   = "-dlt",
            include          = { RuntimeException.class }
    )
    @KafkaListener(
            topics           = "${app.kafka.topic.expense-added}",
            groupId          = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ExpenseAddedEvent> record) {
        ExpenseAddedEvent event = record.value();

        log.info("──────────────────────────────────────────────");
        log.info("📨 Consumed | topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset());
        log.info("   EventId={}", event.getEventId());
        log.info("   ExpenseId={}", event.getExpenseId());
        log.info("   Group={}", event.getGroupName());

        // Check if failure simulation is on — throws if enabled
        failureSimulator.maybeThrow(event.getExpenseId());

        // Normal processing
        notificationService.recalculateAndNotify(event);
    }

    // ── DLT consumer ─────────────────────────────────────
    // Called when all retries are exhausted
    // In production: alert ops team, store in failed_events table,
    // trigger manual review
    @DltHandler
    public void handleDlt(ConsumerRecord<String, ExpenseAddedEvent> record) {
        ExpenseAddedEvent event = record.value();

        log.error("╔══════════════════════════════════════════════╗");
        log.error("║           DEAD LETTER TOPIC HANDLER          ║");
        log.error("╠══════════════════════════════════════════════╣");
        log.error("║ All retries exhausted for this event         ║");
        log.error("║ topic     : {}",  record.topic());
        log.error("║ offset    : {}",  record.offset());
        log.error("║ eventId   : {}",  event != null ? event.getEventId() : "null");
        log.error("║ expenseId : {}",  event != null ? event.getExpenseId() : "null");
        log.error("║ group     : {}",  event != null ? event.getGroupName() : "null");
        log.error("╠══════════════════════════════════════════════╣");
        log.error("║ ACTION REQUIRED:                             ║");
        log.error("║ → Balance NOT recalculated for this expense  ║");
        log.error("║ → Manual intervention may be needed          ║");
        log.error("║ → Check expense-added-dlt topic for payload  ║");
        log.error("╚══════════════════════════════════════════════╝");

        // In production you would:
        // 1. Save to a failed_events DB table
        // 2. Alert ops via PagerDuty / Slack
        // 3. Expose a retry endpoint for manual replay
    }
}
