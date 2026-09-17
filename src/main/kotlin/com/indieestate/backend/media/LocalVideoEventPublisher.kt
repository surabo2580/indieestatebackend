package com.indieestate.backend.media

import com.indieestate.backend.storage.FakeReelObjectStorage
import com.indieestate.backend.storage.ReelObjectStorage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
@ConditionalOnProperty(name = ["app.reels.kafka-enabled"], havingValue = "false", matchIfMissing = true)
class LocalVideoEventPublisher(
    private val worker: MediaWorker,
    storage: ReelObjectStorage,
) : VideoEventPublisher {
    private val executor = if (storage is FakeReelObjectStorage) {
        null
    } else {
        Executors.newFixedThreadPool(2)
    }

    override fun publishUploaded(event: VideoUploadedEvent) {
        val run = Runnable { worker.processUploaded(event) }
        if (executor == null) run.run() else executor.execute(run)
    }
}
