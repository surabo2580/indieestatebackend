package com.indieestate.backend.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import com.indieestate.backend.reel.ReelUploadLog
import com.indieestate.backend.storage.FakeReelObjectStorage
import com.indieestate.backend.storage.ReelObjectStorage
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import java.util.UUID

@Component
class ReelsStorageStartupCheck(
    private val storage: ReelObjectStorage,
    @param:Value("\${app.reels.s3-bucket:}") private val bucket: String,
    @param:Value("\${app.reels.s3-region:ap-south-1}") private val region: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun verifyReelsStorage() {
        if (bucket.isBlank()) {
            log.warn(
                "REELS_S3_BUCKET is empty — reels use in-memory fake storage. " +
                    "Phones will not upload to S3 until you set the bucket and AWS keys in .env",
            )
            return
        }
        val accessKey = System.getenv("AWS_ACCESS_KEY_ID")?.trim().orEmpty()
        val secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")?.trim().orEmpty()
        if (accessKey.isEmpty() || secretKey.isEmpty()) {
            log.error(
                "REELS_S3_BUCKET={} but AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY are missing in .env. " +
                    "POST /api/reels/uploads will fail until IAM keys are configured.",
                bucket,
            )
            return
        }
        runCatching {
            S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build()
                .use { s3 ->
                    s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build())
                }
        }.onSuccess {
            log.info("Reels S3 bucket '{}' is reachable in {}", bucket, region)
            ReelUploadLog.info("startup headBucket OK bucket=$bucket region=$region")
            verifyPutObjectPermission()
        }.onFailure { error ->
            ReelUploadLog.error("startup headBucket FAILED bucket=$bucket region=$region", error)
            log.error(
                "Cannot access reels bucket '{}' in {}. Check bucket name, region, and IAM policy: {}",
                bucket,
                region,
                error.message,
            )
        }
    }

    private fun verifyPutObjectPermission() {
        if (storage is FakeReelObjectStorage) return
        val probeKey = "raw/_startup_probe/${UUID.randomUUID()}.txt"
        runCatching {
            storage.put(probeKey, "ok".toByteArray(), "text/plain")
        }.onSuccess {
            log.info("S3 PutObject succeeded (IAM allows uploads to {})", bucket)
            ReelUploadLog.info("startup putObject OK probeKey=$probeKey bucket=$bucket")
        }.onFailure { error ->
            ReelUploadLog.error("startup putObject FAILED bucket=$bucket probeKey=$probeKey", error)
            log.error(
                "S3 HeadBucket OK but PutObject failed for {}. Add s3:PutObject on arn:aws:s3:::{}/raw/* " +
                    "to the IAM user in AWS_ACCESS_KEY_ID. Error: {}",
                bucket,
                bucket,
                error.message,
            )
        }
    }
}
