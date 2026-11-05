package com.indieestate.backend.media

import com.fasterxml.jackson.databind.ObjectMapper
import com.indieestate.backend.entity.VideoStatus
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.reels.kafka-enabled"], havingValue = "true")
@ConditionalOnProperty(name = ["app.reels.run-media-worker"], havingValue = "true")
class MediaWorkerListener(
    private val mediaProcessing: MediaProcessingService,
    private val objectMapper: ObjectMapper,
    private val kafka: KafkaTemplate<String, String>,
    meterRegistry: MeterRegistry,
    @param:Value("\${app.reels.kafka.processed-topic}") private val processedTopic: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val processedCounter = meterRegistry.counter("reels.transcode.processed")
    private val failedCounter = meterRegistry.counter("reels.transcode.failed")

    @KafkaListener(
        topics = ["\${app.reels.kafka.uploaded-topic}"],
        groupId = "\${app.reels.kafka.worker-group}",
        autoStartup = "\${app.reels.kafka-enabled:false}",
    )
    fun onUploaded(message: String) {
        val event = objectMapper.readValue(message, VideoUploadedEvent::class.java)
        val result = try {
            mediaProcessing.processUploaded(event)
        } catch (error: Exception) {
            log.error("Media worker failed for video {}", event.videoId, error)
            VideoProcessedEvent(event.videoId, event.reelId, VideoStatus.FAILED)
        }
        if (result.status == VideoStatus.FAILED) {
            failedCounter.increment()
        } else {
            processedCounter.increment()
        }
        kafka.send(processedTopic, event.videoId.toString(), objectMapper.writeValueAsString(result))
    }
}
