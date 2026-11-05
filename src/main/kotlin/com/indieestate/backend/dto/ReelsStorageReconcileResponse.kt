package com.indieestate.backend.dto

data class ReelsStorageReconcileResponse(
    val s3MediaPrefixCount: Int,
    val reelsWithVideoIdCount: Int,
    val orphanMediaPrefixesInS3: List<String>,
    val mediaVideoIdsWithoutReelRow: List<String>,
    val videoRowsWithoutReel: List<String>,
)
