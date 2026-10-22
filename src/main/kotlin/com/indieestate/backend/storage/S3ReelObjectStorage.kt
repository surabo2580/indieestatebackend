package com.indieestate.backend.storage

import com.indieestate.backend.reel.ReelUploadLog
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.nio.file.Path
import java.time.Duration

class S3ReelObjectStorage(
    private val s3: S3Client,
    private val presigner: S3Presigner,
    private val bucket: String,
    private val cdnBaseUrl: String,
    private val defaultEncryption: S3DefaultEncryption = S3DefaultEncryption(),
) : ReelObjectStorage {

    override fun presignPut(objectKey: String, contentType: String): PresignedUpload {
        @Suppress("UNUSED_PARAMETER")
        val ignoredContentType = contentType
        val putBuilder = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
        defaultEncryption.applyTo(putBuilder)
        val put = putBuilder.build()
        val request = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .putObjectRequest(put)
            .build()
        val url = presigner.presignPutObject(request).url()
        val headers = defaultEncryption.uploadHeaders()
        ReelUploadLog.debug(
            "presigned PUT bucket=$bucket key=$objectKey host=${url.host} sseHeaders=${headers.keys} expiresIn=15m",
        )
        return PresignedUpload(url = url.toString(), headers = headers)
    }

    override fun exists(objectKey: String): Boolean = try {
        s3.headObject(
            HeadObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build(),
        )
        true
    } catch (_: NoSuchKeyException) {
        false
    } catch (error: S3Exception) {
        if (error.statusCode() == 404) false else throw error
    }

    override fun put(objectKey: String, bytes: ByteArray, contentType: String) {
        val builder = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
        defaultEncryption.applyTo(builder)
        s3.putObject(builder.build(), RequestBody.fromBytes(bytes))
    }

    override fun download(objectKey: String, destination: Path) {
        s3.getObject(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build(),
            ResponseTransformer.toFile(destination),
        )
    }

    override fun upload(objectKey: String, source: Path, contentType: String) {
        val builder = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .contentType(contentType)
        defaultEncryption.applyTo(builder)
        s3.putObject(builder.build(), RequestBody.fromFile(source))
    }

    override fun publicUrl(objectKey: String): String {
        val base = cdnBaseUrl.trimEnd('/')
        val key = objectKey.trimStart('/')
        return "$base/$key"
    }
}
