package com.indieestate.backend.media.transcode

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

@Service
@ConditionalOnProperty(name = ["app.reels.transcode-provider"], havingValue = "ffmpeg", matchIfMissing = true)
class FfmpegVideoTranscodeService(
    @param:Value("\${app.reels.ffmpeg-path:ffmpeg}") private val ffmpegPath: String,
) : VideoTranscodeService {

    override fun transcodeLocal(input: Path, workDir: Path, videoId: UUID): TranscodeResult {
        val source = probeSource(input)
        val ladder = ABR_LADDER.filter { it.height <= source.height + 16 }.ifEmpty { listOf(ABR_LADDER.last()) }
        val hlsRoot = workDir.resolve("hls")
        Files.createDirectories(hlsRoot)
        val built = ladder.map { spec -> encodeRendition(input, hlsRoot, spec) }
        writeMasterPlaylist(hlsRoot.resolve("master.m3u8"), built)
        val poster = workDir.resolve("poster.jpg")
        extractPoster(input, poster)
        return TranscodeResult(
            hlsRoot = hlsRoot,
            poster = poster,
            source = source,
            renditions = built.map {
                TranscodeRenditionSpec(
                    name = it.spec.name,
                    width = it.spec.width,
                    height = it.spec.height,
                    bandwidth = it.spec.bandwidth,
                )
            },
        )
    }

    private fun encodeRendition(input: Path, hlsRoot: Path, spec: AbrRendition): EncodedRendition {
        val outDir = hlsRoot.resolve(spec.name)
        Files.createDirectories(outDir)
        val playlist = outDir.resolve("playlist.m3u8")
        runCommand(
            listOf(
                ffmpegPath, "-y", "-i", input.toString(),
                "-vf", "scale=-2:${spec.height}",
                "-c:v", "libx264", "-preset", "veryfast", "-profile:v", "high", "-level", "4.1",
                "-b:v", spec.videoBitrate, "-maxrate", spec.maxRate, "-bufsize", spec.bufSize,
                "-c:a", "aac", "-b:a", spec.audioBitrate, "-ac", "2",
                "-hls_time", "4", "-hls_playlist_type", "vod",
                "-hls_segment_type", "fmp4",
                "-hls_fmp4_init_filename", "init.mp4",
                "-hls_flags", "independent_segments",
                "-hls_segment_filename", outDir.resolve("segment_%05d.m4s").toString(),
                playlist.toString(),
            ),
        )
        return EncodedRendition(spec)
    }

    private fun writeMasterPlaylist(path: Path, renditions: List<EncodedRendition>) {
        val body = buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-INDEPENDENT-SEGMENTS")
            renditions.forEach { encoded ->
                val spec = encoded.spec
                appendLine()
                appendLine("#EXT-X-STREAM-INF:BANDWIDTH=${spec.bandwidth},RESOLUTION=${spec.width}x${spec.height}")
                appendLine("${spec.name}/playlist.m3u8")
            }
        }
        Files.writeString(path, body)
    }

    private fun extractPoster(input: Path, poster: Path) {
        runCommand(
            listOf(
                ffmpegPath, "-y", "-ss", "0", "-i", input.toString(),
                "-frames:v", "1", "-q:v", "4",
                poster.toString(),
            ),
        )
    }

    private fun probeSource(input: Path): TranscodeSourceInfo {
        val output = runCommandCapture(
            listOf(
                ffprobePath(), "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height:format=duration",
                "-of", "csv=p=0:s=,",
                input.toString(),
            ),
        )
        val parts = output.trim().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return TranscodeSourceInfo(
            width = parts.getOrNull(0)?.toIntOrNull() ?: 1280,
            height = parts.getOrNull(1)?.toIntOrNull() ?: 720,
            duration = parts.getOrNull(2)?.toDoubleOrNull(),
        )
    }

    private fun ffprobePath(): String {
        if (ffmpegPath.endsWith("ffmpeg")) return ffmpegPath.removeSuffix("ffmpeg") + "ffprobe"
        return "ffprobe"
    }

    private fun runCommand(command: List<String>) {
        runCommandCapture(command)
    }

    private fun runCommandCapture(command: List<String>): String {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(10, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw IllegalStateException("${command.first()} timed out: $output")
        }
        if (process.exitValue() != 0) {
            throw IllegalStateException("${command.first()} failed: $output")
        }
        return output
    }

    private data class EncodedRendition(val spec: AbrRendition)

    private data class AbrRendition(
        val name: String,
        val width: Int,
        val height: Int,
        val videoBitrate: String,
        val maxRate: String,
        val bufSize: String,
        val audioBitrate: String,
        val bandwidth: Int,
    )

    companion object {
        private val ABR_LADDER = listOf(
            AbrRendition("1080p", 1920, 1080, "8000k", "8560k", "12000k", "128k", 8_000_000),
            AbrRendition("720p", 1280, 720, "5000k", "5350k", "7500k", "128k", 5_000_000),
            AbrRendition("480p", 854, 480, "2500k", "2675k", "3750k", "96k", 2_500_000),
            AbrRendition("360p", 640, 360, "1200k", "1284k", "1800k", "64k", 1_200_000),
        )
    }
}
