package com.example.archivetok.data.model

import com.google.gson.annotations.SerializedName

data class MetadataResponse(
    val files: List<ArchiveFile>,
    val metadata: ArchiveMetadata,
    val server: String,
    val dir: String,
    val reviews: List<ArchiveReview>?
)

data class ArchiveMetadata(
    val identifier: String,
    val title: String?,
    val description: String?,
    val mediatype: String?
)

data class ArchiveFile(
    val name: String,
    val source: String,
    val format: String,
    val size: String?,
    val mtime: String?
)

data class ArchiveReview(
    val reviewer: String?,
    val reviewdate: String?,
    val reviewbody: String?
)
