package com.example.crudfirebaseapp.rickandmorty.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CharacterDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String,
    @Json(name = "species") val species: String,
    @Json(name = "gender") val gender: String,
    @Json(name = "origin") val origin: OriginDto,
    @Json(name = "location") val location: LocationDto,
    @Json(name = "image") val image: String
)

@JsonClass(generateAdapter = true)
data class OriginDto(@Json(name = "name") val name: String)

@JsonClass(generateAdapter = true)
data class LocationDto(@Json(name = "name") val name: String)