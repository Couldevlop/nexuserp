package com.nexuserp.payment.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Déclaration explicite des topics paiement (créés via KafkaAdmin au démarrage).
 * Évite la dépendance au réglage broker auto.create.topics.enable.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic paymentInitiatedTopic() {
        return TopicBuilder.name("nexus.payment.initiated").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentSucceededTopic() {
        return TopicBuilder.name("nexus.payment.succeeded").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name("nexus.payment.failed").partitions(3).replicas(1).build();
    }
}
