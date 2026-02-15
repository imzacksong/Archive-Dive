package com.example.archivetok.data.remote

import com.example.archivetok.data.model.MetadataResponse
import com.example.archivetok.data.model.SearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArchiveService {

    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "description", "mediatype", "year", "subject"),
        @Query("sort[]") sort: String = "downloads desc",
        @Query("rows") rows: Int = 50,
        @Query("page") page: Int,
        @Query("output") output: String = "json"
    ): SearchResponse

    @GET("metadata/{identifier}")
    suspend fun getMetadata(
        @Path("identifier") identifier: String
    ): MetadataResponse
}
