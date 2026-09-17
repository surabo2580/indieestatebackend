package com.indieestate.backend.media

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper

@Component
@ConditionalOnProperty(name = ["app.reels.kafka-enabled"], havingValue = "true")
class KafkaVideoEventPublisher(
    private val kafka: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    @param:Value("\${app.reels.kafka.uploaded-topic}") private val uploadedTopic: String,
) : VideoEventPublisher {
    override fun publishUploaded(event: VideoUploadedEvent) {
        kafka.send(uploadedTopic, event.videoId.toString(), objectMapper.writeValueAsString(event))
    }
}
