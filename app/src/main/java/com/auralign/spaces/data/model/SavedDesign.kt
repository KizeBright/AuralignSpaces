package com.auralign.spaces.data.model

import com.google.firebase.Timestamp

data class SavedDesign(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val roomType: String = "",
    val widthM: Double = 0.0,
    val lengthM: Double = 0.0,
    val heightM: Double = 0.0,
    val budget: Double = 0.0,
    val budgetUsed: Double = 0.0,
    val placedItems: List<PlacedObjectDto> = emptyList(),
    /**
     * DB-driven material selection (preferred).
     */
    val wallPaintId: String = "",
    /**
     * Legacy field: keep for backward compatibility with older documents.
     */
    val wallColorHex: String = "",
    val floorMaterialId: String = "",
    val thumbnailUrl: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
