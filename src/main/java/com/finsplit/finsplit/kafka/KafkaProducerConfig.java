package com.finsplit.finsplit.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.topic.expense-added}")
    private String expenseAddedTopic;

    @Bean
    public ProducerFactory<String, ExpenseAddedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, ExpenseAddedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Auto-creates the topic on startup if it doesn't exist
    @Bean
    public NewTopic expenseAddedTopic() {
        return TopicBuilder.name(expenseAddedTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Value("${app.kafka.topic.expense-added-dlt}")
    private String expenseAddedDltTopic;

    @Bean
    public NewTopic expenseAddedDltTopic() {
        return TopicBuilder.name(expenseAddedDltTopic)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
