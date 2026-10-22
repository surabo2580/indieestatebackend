package com.indieestate.backend.config

import com.indieestate.backend.reel.ReelUploadLog
import com.indieestate.backend.storage.FakeReelObjectStorage
import com.indieestate.backend.storage.ReelObjectStorage
import com.indieestate.backend.storage.S3DefaultEncryption
import com.indieestate.backend.storage.S3ReelObjectStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class ReelStorageConfig(
    @param:Value("\${app.reels.s3-bucket:}") private val bucket: String,
    @param:Value("\${app.reels.s3-region:ap-south-1}") private val region: String,
    @param:Value("\${app.reels.cdn-base-url:}") private val cdnBaseUrl: String,
) {
    private val useS3: Boolean get() = bucket.isNotBlank()

    @Bean
    fun reelObjectStorage(): ReelObjectStorage {
        if (!useS3) {
            return FakeReelObjectStorage(cdnBaseUrl.ifBlank { "https://cdn.test" })
        }
        val awsRegion = Region.of(region)
        val credentials = DefaultCredentialsProvider.builder().build()
        val s3 = S3Client.builder()
            .region(awsRegion)
            .credentialsProvider(credentials)
            .build()
        val presigner = S3Presigner.builder()
            .region(awsRegion)
            .credentialsProvider(credentials)
            .build()
        val defaultEncryption = loadDefaultEncryption(s3, bucket)
        return S3ReelObjectStorage(
            s3 = s3,
            presigner = presigner,
            bucket = bucket,
            cdnBaseUrl = cdnBaseUrl.trimEnd('/'),
            defaultEncryption = defaultEncryption,
        )
    }

    private fun loadDefaultEncryption(s3: S3Client, bucket: String): S3DefaultEncryption {
        return runCatching {
            val rule = s3.getBucketEncryption(
                GetBucketEncryptionRequest.builder().bucket(bucket).build(),
            ).serverSideEncryptionConfiguration()?.rules()?.firstOrNull()?.applyServerSideEncryptionByDefault()
            S3DefaultEncryption(
                algorithm = rule?.sseAlgorithm(),
                kmsKeyId = rule?.kmsMasterKeyID(),
            )
        }.getOrElse { error ->
            ReelUploadLog.debug("bucket encryption config not read ($bucket): ${error.message}")
            S3DefaultEncryption()
        }.also { encryption ->
            ReelUploadLog.info(
                "bucket default encryption algorithm=${encryption.algorithm} " +
                    "kmsKeyConfigured=${!encryption.kmsKeyId.isNullOrBlank()}",
            )
        }
    }
}
