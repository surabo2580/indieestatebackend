package com.indieestate.backend.service

import com.indieestate.backend.controller.ApiException
import com.indieestate.backend.dto.CompleteReelRequest
import com.indieestate.backend.dto.CreateReelRequest
import com.indieestate.backend.dto.CreateReelUploadRequest
import com.indieestate.backend.dto.CreateReelUploadResponse
import com.indieestate.backend.dto.ReelCommentResponse
import com.indieestate.backend.dto.ReelResponse
import com.indieestate.backend.entity.ReelEntity
import com.indieestate.backend.entity.ReelStatus
import com.indieestate.backend.entity.UserEntity
import com.indieestate.backend.entity.VideoEntity
import com.indieestate.backend.entity.VideoStatus
import com.indieestate.backend.feed.ReelFeedCache
import com.indieestate.backend.media.VideoObjectKeys
import com.indieestate.backend.repository.ReelRepository
import com.indieestate.backend.repository.UserRepository
import com.indieestate.backend.repository.VideoRepository
import com.indieestate.backend.storage.ReelObjectStorage
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ReelService(
    private val reelRepository: ReelRepository,
    private val userRepository: UserRepository,
    private val videoRepository: VideoRepository,
    private val reelMediaStore: ReelMediaStore,
    private val storage: ReelObjectStorage,
    private val feedCache: ReelFeedCache,
) {

    @Transactional
    fun create(userId: UUID, request: CreateReelRequest): ReelResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "User not found")
        }
        val reel = reelRepository.save(
            ReelEntity(
                userId = userId,
                title = request.title.trim(),
                caption = request.caption.trim(),
                category = request.category.trim(),
                priceLabel = request.priceLabel.trim(),
                location = request.location.trim(),
                videoUrl = request.videoUrl.trim(),
                thumbnailUrl = request.thumbnailUrl?.trim()?.takeIf { it.isNotEmpty() },
                thumbnailHex = normalizeHex(request.thumbnailHex) ?: hexForCategory(request.category),
                status = ReelStatus.READY,
            ),
        )
        feedCache.evict()
        return reel.toResponse(user)
    }

    @Transactional
    fun createUpload(userId: UUID, request: CreateReelUploadRequest): CreateReelUploadResponse {
        userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "User not found")
        }
        val fileName = request.fileName.trim()
        val ext = extensionOf(fileName)
        if (ext !in ALLOWED_VIDEO_EXTENSIONS) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Video must be mp4, mov, webm, or m4v")
        }
        val contentType = request.contentType?.trim()?.takeIf { it.isNotEmpty() } ?: contentTypeFor(ext)
        val videoId = UUID.randomUUID()
        val objectKey = VideoObjectKeys.original(videoId, ext)
        videoRepository.save(
            VideoEntity(
                id = videoId,
                ownerId = userId,
                status = VideoStatus.UPLOADING,
                originalKey = objectKey,
            ),
        )
        return CreateReelUploadResponse(
            videoId = videoId,
            objectKey = objectKey,
            uploadUrl = storage.presignPut(objectKey, contentType),
            contentType = contentType,
        )
    }

    @Transactional
    fun complete(userId: UUID, request: CompleteReelRequest): ReelResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "User not found")
        }
        val video = videoRepository.findById(request.videoId).orElseThrow {
            ApiException(HttpStatus.BAD_REQUEST, "Unknown video upload")
        }
        if (video.ownerId != userId) {
            throw ApiException(HttpStatus.FORBIDDEN, "This upload belongs to another user")
        }
        val objectKey = request.objectKey.trim()
        if (objectKey != video.originalKey) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Invalid upload key")
        }
        if (!storage.exists(objectKey)) {
            throw ApiException(HttpStatus.BAD_REQUEST, "Upload the video before completing the reel")
        }
        reelRepository.findFirstByVideoId(video.id)?.let { existing ->
            return existing.toResponse(user)
        }
        video.status = VideoStatus.UPLOADED
        videoRepository.save(video)
        val reel = reelRepository.save(
            ReelEntity(
                userId = userId,
                title = request.title.trim(),
                caption = request.caption.trim(),
                category = request.category.trim(),
                priceLabel = request.priceLabel.trim(),
                location = request.location.trim(),
                videoUrl = "",
                objectKey = objectKey,
                videoId = video.id,
                thumbnailHex = normalizeHex(request.thumbnailHex) ?: hexForCategory(request.category),
                status = ReelStatus.PROCESSING,
            ),
        )
        return reel.toResponse(user)
    }

    @Transactional
    fun createFromUpload(
        userId: UUID,
        title: String,
        caption: String,
        category: String,
        priceLabel: String,
        location: String,
        thumbnailHex: String?,
        video: MultipartFile,
    ): ReelResponse {
        val user = userRepository.findById(userId).orElseThrow {
            ApiException(HttpStatus.UNAUTHORIZED, "User not found")
        }
        val filename = reelMediaStore.save(video)
        val reel = reelRepository.save(
            ReelEntity(
                userId = userId,
                title = title.trim(),
                caption = caption.trim(),
                category = category.trim(),
                priceLabel = priceLabel.trim(),
                location = location.trim(),
                videoUrl = "/api/reels/media/$filename",
                thumbnailHex = normalizeHex(thumbnailHex) ?: hexForCategory(category),
                status = ReelStatus.READY,
            ),
        )
        feedCache.evict()
        return reel.toResponse(user)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): ReelResponse? {
        val reel = reelRepository.findById(id).orElse(null) ?: return null
        val user = userRepository.findById(reel.userId).orElse(null)
        return reel.toResponse(user)
    }

    @Transactional(readOnly = true)
    fun listActive(): List<ReelResponse> {
        feedCache.get()?.let { return it }
        val reels = reelRepository.findAllByIsActiveTrueAndStatusOrderByCreatedAtDesc(ReelStatus.READY)
        if (reels.isEmpty()) {
            feedCache.put(emptyList())
            return emptyList()
        }
        val users = userRepository.findAllById(reels.map { it.userId }.toSet())
            .associateBy { it.id }
        val response = reels.map { reel ->
            reel.toResponse(users[reel.userId])
        }
        feedCache.put(response)
        return response
    }
}

private val ALLOWED_VIDEO_EXTENSIONS = setOf("mp4", "mov", "webm", "m4v")

private fun extensionOf(name: String): String =
    name.substringAfterLast('.', "").lowercase()

private fun contentTypeFor(ext: String): String = when (ext) {
    "mov" -> "video/quicktime"
    "webm" -> "video/webm"
    "m4v", "mp4" -> "video/mp4"
    else -> "video/mp4"
}

private fun ReelEntity.toResponse(user: UserEntity?) = ReelResponse(
    id = id,
    videoId = videoId,
    userId = userId,
    sellerName = user?.name?.takeIf { it.isNotBlank() } ?: "Seller",
    sellerInitials = initials(user?.name),
    category = category,
    title = title,
    caption = caption,
    priceLabel = priceLabel,
    location = location,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    thumbnailHex = thumbnailHex,
    likeCount = likeCount,
    commentCount = commentCount,
    isLiked = false,
    comments = emptyList<ReelCommentResponse>(),
    status = status,
    createdAt = createdAt,
)

private fun initials(name: String?): String {
    val parts = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "IE"
    return parts.take(2).joinToString("") { it.first().uppercase() }
}

private fun normalizeHex(value: String?): String? {
    val cleaned = value?.trim().orEmpty()
    if (cleaned.isEmpty()) return null
    val hex = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
    return hex.takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) } ?: hex
}

private fun hexForCategory(category: String): String = when (category.trim().lowercase()) {
    "cars", "car" -> "#1B3A4B"
    "properties", "property", "home" -> "#3D2C2E"
    "bikes", "bike" -> "#2C1F3D"
    "jobs", "job" -> "#1F3D2C"
    else -> "#1A2744"
}
