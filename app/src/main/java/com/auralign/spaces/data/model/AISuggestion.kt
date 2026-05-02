package com.auralign.spaces.data.model

data class AISuggestion(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val style: String = "",
    val recommendedItems: List<String> = emptyList(),
    val wallColor: String = "#FFFFFF",
    val floorMaterial: String = "",
    val estimatedCostInr: Int = 0
)
