package com.example.archivetok.data.model

import com.google.gson.annotations.SerializedName

data class MetadataResponse(
    val files: List<ArchiveFile>,
    val metadata: ArchiveMetadata,
    val server: String,
    val dir: String
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
