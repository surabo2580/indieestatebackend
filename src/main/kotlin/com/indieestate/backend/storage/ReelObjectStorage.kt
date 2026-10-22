package com.indieestate.backend.storage

import java.nio.file.Path

interface ReelObjectStorage {
    fun presignPut(objectKey: String, contentType: String): PresignedUpload
    fun exists(objectKey: String): Boolean
    fun put(objectKey: String, bytes: ByteArray, contentType: String)
    fun download(objectKey: String, destination: Path)
    fun upload(objectKey: String, source: Path, contentType: String)
    fun publicUrl(objectKey: String): String
}
