package com.auralign.spaces.data.model

import com.auralign.spaces.domain.model.FurnitureCategory

data class FurnitureItem(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val category: FurnitureCategory = FurnitureCategory.FURNITURE,
    val price: Double = 0.0,
    val emoji: String = "Ã°Å¸â€ºâ€¹Ã¯Â¸Â",
    val modelId: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val widthM: Double = 0.0,
    val depthM: Double = 0.0,
    val colorOptions: List<String> = emptyList(),
    val rating: Float = 4.5f,
    val modelUrl: String = "" // Added modelUrl at the end to prevent positional argument breaking
)
