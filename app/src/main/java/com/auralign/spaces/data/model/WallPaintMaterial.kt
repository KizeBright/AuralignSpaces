package com.auralign.spaces.data.model

data class WallPaintMaterial(
    val id: String = "",
    val name: String = "",
    val hex: String = "#F5F5F0",
    val price: Double = 0.0,
    val roughness: Float = 0.95f,
    val reflectance: Float = 0.3f,
    val thumbnailUrl: String = ""
)

