package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Component
class ReelMediaStore(
    @param:Value("\${app.reels.upload-dir:uploads/reels}") private val uploadDir: String,
) {
    private val root: Path = Path.of(uploadDir).toAbsolutePath().normalize()

    init {
        Files.createDirectories(root)
    }

    fun save(file: MultipartFile): String {
        if (file.isEmpty) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Video file is required")
        }
        val original = file.originalFilename.orEmpty()
        val ext = extensionOf(original)
        if (ext !in ALLOWED_EXTENSIONS) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Video must be mp4, mov, or webm")
        }
        val filename = "${UUID.randomUUID()}.$ext"
        val target = root.resolve(filename)
        file.transferTo(target)
        return filename
    }

    fun resolve(filename: String): Path {
        if (!SAFE_NAME.matches(filename)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Invalid media file")
        }
        val path = root.resolve(filename).normalize()
        if (!path.startsWith(root) || !Files.exists(path)) {
            throw ApiException(HttpStatus.NOT_FOUND, "Video not found")
        }
        return path
    }

    companion object {
        private val ALLOWED_EXTENSIONS = setOf("mp4", "mov", "webm", "m4v")
        private val SAFE_NAME = Regex("^[a-fA-F0-9-]+\\.(mp4|mov|webm|m4v)$")

        private fun extensionOf(name: String): String =
            name.substringAfterLast('.', "").lowercase()
    }
}
