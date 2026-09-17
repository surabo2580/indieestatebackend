package com.indieestate.backend.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@ConditionalOnProperty(name = ["app.reels.kafka-enabled"], havingValue = "true")
class KafkaMediaConfig
