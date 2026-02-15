package com.example.archivetok.data.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    val response: ResponseData
)

data class ResponseData(
    val docs: List<ArchiveItem>,
    val numFound: Int
)

data class ArchiveItem(
    val identifier: String,
    @com.google.gson.annotations.JsonAdapter(com.example.archivetok.data.remote.StringOrListDeserializer::class)
    val title: String?,
    @com.google.gson.annotations.JsonAdapter(com.example.archivetok.data.remote.StringOrListDeserializer::class)
    val description: String?,
    val mediatype: String?,
    @SerializedName("year")
    val year: String?, // Year can be string or int in JSON
    @com.google.gson.annotations.JsonAdapter(com.example.archivetok.data.remote.StringListDeserializer::class)
    val subject: List<String>?
)
