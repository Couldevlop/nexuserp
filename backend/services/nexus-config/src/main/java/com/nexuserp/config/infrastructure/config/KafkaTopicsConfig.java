package com.nexuserp.config.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Déclaration explicite du topic de configuration (créé via KafkaAdmin au démarrage).
 * Évite la dépendance au réglage broker auto.create.topics.enable.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic configChangedTopic() {
        return TopicBuilder.name("nexus.config.changed").partitions(3).replicas(1).build();
    }
}
