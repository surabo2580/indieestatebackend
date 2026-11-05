package com.indieestate.backend.media

import org.springframework.stereotype.Service

/**
 * Media processing service (steps 4–5 in [docs/ARCHITECTURE.md]).
 * Consumes [VideoUploadedEvent] from Kafka and writes HLS renditions to object storage.
 */
@Service
class MediaProcessingService(
    private val worker: MediaWorker,
) {
    fun processUploaded(event: VideoUploadedEvent): VideoProcessedEvent =
        worker.processUploaded(event)
}
