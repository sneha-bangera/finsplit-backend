package com.finsplit.finsplit.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, ExpenseAddedEvent> consumerFactory() {
        JsonDeserializer<ExpenseAddedEvent> deserializer =
                new JsonDeserializer<>(ExpenseAddedEvent.class, false);
        deserializer.addTrustedPackages("com.finsplit.kafka");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config,
                new StringDeserializer(), deserializer);
    }

//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, ExpenseAddedEvent>
//    kafkaListenerContainerFactory(
//            ConsumerFactory<String, ExpenseAddedEvent> consumerFactory,
//            KafkaTemplate<String, ExpenseAddedEvent> kafkaTemplate) {
//
//        var factory =
//                new ConcurrentKafkaListenerContainerFactory<String, ExpenseAddedEvent>();
//        factory.setConsumerFactory(consumerFactory);
//
//        // ── Retry + Dead Letter ───────────────────────────
//        // Retry 3 times with 2 second gap, then send to DLT
//        DeadLetterPublishingRecoverer recoverer =
//                new DeadLetterPublishingRecoverer(kafkaTemplate);
//
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                recoverer,
//                new FixedBackOff(2000L, 3L) // 2s interval, 3 attempts
//        );
//
//        factory.setCommonErrorHandler(errorHandler);
//
//        return factory;
//    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ExpenseAddedEvent>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, ExpenseAddedEvent> consumerFactory) {

        var factory =
                new ConcurrentKafkaListenerContainerFactory<String, ExpenseAddedEvent>();
        factory.setConsumerFactory(consumerFactory);

        // @RetryableTopic on the listener handles retry + DLT automatically
        // No manual error handler needed here

        return factory;
}
}
