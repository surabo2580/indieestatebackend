package com.indieestate.backend.media.transcode

import java.nio.file.Path
import java.util.UUID

data class TranscodeSourceInfo(
    val width: Int,
    val height: Int,
    val duration: Double?,
)

data class TranscodeRenditionSpec(
    val name: String,
    val width: Int,
    val height: Int,
    val bandwidth: Int,
)

data class TranscodeResult(
    val hlsRoot: Path,
    val poster: Path,
    val source: TranscodeSourceInfo,
    val renditions: List<TranscodeRenditionSpec>,
)

interface VideoTranscodeService {
    fun transcodeLocal(input: Path, workDir: Path, videoId: UUID): TranscodeResult
}
