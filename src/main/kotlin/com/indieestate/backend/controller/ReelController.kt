package com.indieestate.backend.controller

import com.indieestate.backend.dto.CompleteReelRequest
import com.indieestate.backend.dto.CreateReelRequest
import com.indieestate.backend.dto.CreateReelUploadRequest
import com.indieestate.backend.dto.CreateReelUploadResponse
import com.indieestate.backend.dto.ReelResponse
import com.indieestate.backend.security.UserPrincipal
import com.indieestate.backend.media.VideoEventPublisher
import com.indieestate.backend.media.VideoUploadedEvent
import com.indieestate.backend.service.ReelMediaStore
import com.indieestate.backend.service.ReelService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRange
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.RandomAccessFile
import kotlin.math.min

@RestController
@RequestMapping("/api/reels")
@CrossOrigin(origins = ["*"])
@Tag(name = "Reels")
class ReelController(
    private val reelService: ReelService,
    private val reelMediaStore: ReelMediaStore,
    private val videoEventPublisher: VideoEventPublisher,
) {

    @GetMapping
    @Operation(summary = "List active video reels for the Play screen")
    fun list(): List<ReelResponse> = reelService.listActive()

    @GetMapping("/media/{filename}")
    @Operation(summary = "Stream an uploaded reel video")
    fun media(
        @PathVariable filename: String,
        request: HttpServletRequest,
    ): ResponseEntity<StreamingResponseBody> {
        val file = reelMediaStore.resolve(filename).toFile()
        val contentType = when (filename.substringAfterLast('.').lowercase()) {
            "mp4", "m4v" -> MediaType.parseMediaType("video/mp4")
            "webm" -> MediaType.parseMediaType("video/webm")
            "mov" -> MediaType.parseMediaType("video/quicktime")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
        val length = file.length()
        var start = 0L
        var end = (length - 1).coerceAtLeast(0L)
        var status = HttpStatus.OK
        val rangeHeader = request.getHeader(HttpHeaders.RANGE)
        if (!rangeHeader.isNullOrBlank() && length > 0L) {
            val range = try {
                HttpRange.parseRanges(rangeHeader).firstOrNull()
            } catch (_: IllegalArgumentException) {
                null
            }
            if (range == null) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */$length")
                    .build()
            }
            start = range.getRangeStart(length)
            end = range.getRangeEnd(length)
            status = HttpStatus.PARTIAL_CONTENT
        }
        if (length == 0L || start < 0L || end < start || start >= length) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */$length")
                .build()
        }
        val contentLength = end - start + 1
        val body = StreamingResponseBody { output ->
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var remaining = contentLength
                while (remaining > 0) {
                    val read = raf.read(buffer, 0, min(buffer.size.toLong(), remaining).toInt())
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }
        val response = ResponseEntity.status(status)
            .contentType(contentType)
            .contentLength(contentLength)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
        if (status == HttpStatus.PARTIAL_CONTENT) {
            response.header(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$length")
        }
        return response.body(body)
    }

    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Get a presigned S3 PUT URL for a gallery video")
    fun createUpload(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateReelUploadRequest,
    ): CreateReelUploadResponse = reelService.createUpload(principal.userId, request)

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Save reel metadata after the client uploaded the video to S3")
    fun complete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CompleteReelRequest,
    ): ReelResponse {
        val reel = reelService.complete(principal.userId, request)
        val videoId = reel.videoId
        if (videoId != null) {
            videoEventPublisher.publishUploaded(
                VideoUploadedEvent(
                    videoId = videoId,
                    reelId = reel.id,
                    originalKey = request.objectKey.trim(),
                ),
            )
        }
        return reelService.getById(reel.id) ?: reel
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Post a video reel from a URL")
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: CreateReelRequest,
    ): ReelResponse = reelService.create(principal.userId, request)

    @PostMapping(path = ["/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Post a video reel from the gallery")
    fun createFromGallery(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam title: String,
        @RequestParam caption: String,
        @RequestParam category: String,
        @RequestParam priceLabel: String,
        @RequestParam location: String,
        @RequestParam(required = false) thumbnailHex: String?,
        @RequestPart("video") video: MultipartFile,
    ): ReelResponse = reelService.createFromUpload(
        userId = principal.userId,
        title = title,
        caption = caption,
        category = category,
        priceLabel = priceLabel,
        location = location,
        thumbnailHex = thumbnailHex,
        video = video,
    )
}
