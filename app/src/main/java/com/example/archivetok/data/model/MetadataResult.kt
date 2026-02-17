package com.example.archivetok.data.model

data class MetadataResult(
    val videoUrls: List<String>,
    val isMultiPart: Boolean = false,
    val fileCount: Int = 0,
    val reviews: List<ArchiveReview> = emptyList()
)
