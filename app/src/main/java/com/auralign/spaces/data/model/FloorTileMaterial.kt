package com.auralign.spaces.data.model

data class FloorTileMaterial(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val textureUrl: String = "",
    val roughness: Float = 0.85f,
    val reflectance: Float = 0.4f,
    val uvScale: Float = 6.0f,
    val thumbnailUrl: String = ""
)

