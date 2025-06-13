package com.example.crudfirebaseapp.rickandmorty.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CharacterApiResponse(
    @Json(name = "info") val info: Info,
    @Json(name = "results") val results: List<CharacterDto>
)

@JsonClass(generateAdapter = true)
data class Info(
    @Json(name = "count") val count: Int,
    @Json(name = "pages") val pages: Int,
    @Json(name = "next") val next: String?,
    @Json(name = "prev") val prev: String?
)