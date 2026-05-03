package com.auralign.spaces.domain.model

import com.auralign.spaces.data.model.FurnitureItem
import java.util.Locale

fun FurnitureItem.isVisibleInRoom(roomType: String): Boolean {
    if (category == FurnitureCategory.FLOOR || category == FurnitureCategory.WALL) return true

    val room = roomType.lowercase(Locale.ROOT)
    val kind = catalogKind()

    return when {
        room.contains("bath") -> kind in setOf("bathtub", "shower")
        room.contains("kitchen") -> kind in setOf("oven", "fridge", "chair", "table")
        room.contains("office") -> kind in setOf("chair", "office_table", "wardrobe", "plant")
        room.contains("living") -> kind in setOf("chair", "table", "sofa", "plant")
        room.contains("dining") || room.contains("dinning") -> kind in setOf("dining_table", "plant", "chair")
        room.contains("bedroom") -> kind in setOf("table", "bed", "chair", "wardrobe")
        else -> true
    }
}

private fun FurnitureItem.catalogKind(): String {
    val searchable = listOf(id, name, modelId, description)
        .joinToString(" ")
        .lowercase(Locale.ROOT)

    return when {
        searchable.contains("bathtub") || searchable.contains("bath tub") ||
            searchable.contains("freestanding tub") -> "bathtub"
        searchable.contains("shower") -> "shower"
        searchable.contains("fridge") || searchable.contains("refrigerator") -> "fridge"
        searchable.contains("oven") || searchable.contains("kitchen table") -> "oven"
        searchable.contains("dining") || searchable.contains("dinning") -> "dining_table"
        searchable.contains("office table") || searchable.contains("computer table") ||
            searchable.contains("desk") -> "office_table"
        searchable.contains("wardrobe") || searchable.contains("wadrobe") -> "wardrobe"
        searchable.contains("sofa") -> "sofa"
        searchable.contains("plant") || category == FurnitureCategory.PLANTS -> "plant"
        searchable.contains("bed") -> "bed"
        searchable.contains("chair") || searchable.contains("armchair") -> "chair"
        searchable.contains("table") -> "table"
        else -> "other"
    }
}
