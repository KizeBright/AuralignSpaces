package com.auralign.spaces.data.model

data class PlacedObject(
    val instanceId: String = "",
    val item: FurnitureItem = FurnitureItem(),
    /**
     * True when this object has been anchored in AR space (tap-to-place).
     * Until placed, it shouldn't be rendered in 3D.
     */
    val isPlaced: Boolean = false,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    /**
     * Anchor/world rotation quaternion (ARCore Pose).
     * Stored so saved designs can be reconstructed in the same real-world orientation.
     */
    val rotQx: Float = 0f,
    val rotQy: Float = 0f,
    val rotQz: Float = 0f,
    val rotQw: Float = 1f,
    val rotationDeg: Float = 0f,
    val scale: Float = 1f,
    val selectedColorHex: String = ""
)

data class PlacedObjectDto(
    val instanceId: String = "",
    val itemId: String = "",
    val isPlaced: Boolean = false,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val rotQx: Float = 0f,
    val rotQy: Float = 0f,
    val rotQz: Float = 0f,
    val rotQw: Float = 1f,
    val rotationDeg: Float = 0f,
    val scale: Float = 1f,
    val selectedColorHex: String = ""
)
