package com.indieestate.backend.media

fun interface VideoEventPublisher {
    fun publishUploaded(event: VideoUploadedEvent)
}
