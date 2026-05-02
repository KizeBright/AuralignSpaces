package com.auralign.spaces.domain.model

enum class RoomType(
    val label: String,
    val emoji: String,
    val defaultW: Double,
    val defaultL: Double,
    val defaultH: Double
) {
    LIVING("Living Room", "🛋️", 5.0, 4.0, 3.0),
    BEDROOM("Bedroom", "🛏️", 4.0, 3.5, 3.0),
    KITCHEN("Kitchen", "🍳", 3.0, 3.0, 2.8),
    BATHROOM("Bathroom", "🚿", 2.5, 2.0, 2.5),
    OFFICE("Home Office", "💼", 3.5, 3.0, 2.8),
    DINING("Dining Room", "🍽️", 4.0, 3.5, 3.0)
}

enum class FurnitureCategory(val label: String, val emoji: String) {
    FURNITURE("Furniture", "🛋️"),
    DECOR("Decor", "🏺"),
    LIGHTING("Lighting", "💡"),
    RUGS("Rugs", "🧶"),
    WALL_ART("Wall Art", "🖼️"),
    STORAGE("Storage", "📂"),
    PLANTS("Plants", "🪴"),
    FLOOR("Floor", "🏠"),
    WALL("Wall", "🏢")
}
