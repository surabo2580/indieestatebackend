package com.indieestate.backend.storage

import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.ServerSideEncryption

data class S3DefaultEncryption(
    val algorithm: ServerSideEncryption? = null,
    val kmsKeyId: String? = null,
) {
    fun uploadHeaders(): Map<String, String> = when (algorithm) {
        ServerSideEncryption.AWS_KMS -> buildMap {
            put("x-amz-server-side-encryption", "aws:kms")
            kmsKeyId?.takeIf { it.isNotBlank() }?.let { put("x-amz-server-side-encryption-aws-kms-key-id", it) }
        }
        ServerSideEncryption.AES256 -> mapOf("x-amz-server-side-encryption" to "AES256")
        else -> emptyMap()
    }

    fun applyTo(builder: PutObjectRequest.Builder) {
        when (algorithm) {
            ServerSideEncryption.AWS_KMS -> {
                builder.serverSideEncryption(ServerSideEncryption.AWS_KMS)
                kmsKeyId?.takeIf { it.isNotBlank() }?.let { builder.ssekmsKeyId(it) }
            }
            ServerSideEncryption.AES256 -> builder.serverSideEncryption(ServerSideEncryption.AES256)
            else -> Unit
        }
    }
}
