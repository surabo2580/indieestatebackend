package com.indieestate.backend.media.transcode

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["app.reels.transcode-provider"], havingValue = "mediaconvert")
class MediaConvertVideoTranscodeService : VideoTranscodeService {
    override fun transcodeLocal(input: Path, workDir: Path, videoId: UUID): TranscodeResult {
        throw UnsupportedOperationException(
            "MediaConvert transcode is not implemented yet. Set REELS_TRANSCODE_PROVIDER=ffmpeg.",
        )
    }
}
