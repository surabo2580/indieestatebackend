package com.indieestate.backend.storage

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class FakeReelObjectStorage(
    private val cdnBaseUrl: String = "https://cdn.test",
) : ReelObjectStorage {
    private val objects = ConcurrentHashMap<String, ByteArray>()

    override fun presignPut(objectKey: String, contentType: String): String =
        "https://cdn.test/upload/${objectKey.trimStart('/')}"

    override fun exists(objectKey: String): Boolean = objects.containsKey(objectKey)

    override fun put(objectKey: String, bytes: ByteArray, contentType: String) {
        objects[objectKey] = bytes
    }

    override fun download(objectKey: String, destination: Path) {
        val bytes = objects[objectKey] ?: error("Object not found: $objectKey")
        Files.createDirectories(destination.parent)
        Files.write(destination, bytes)
    }

    override fun upload(objectKey: String, source: Path, contentType: String) {
        objects[objectKey] = Files.readAllBytes(source)
    }

    override fun publicUrl(objectKey: String): String {
        val base = cdnBaseUrl.trimEnd('/')
        val key = objectKey.trimStart('/')
        return "$base/$key"
    }
}
