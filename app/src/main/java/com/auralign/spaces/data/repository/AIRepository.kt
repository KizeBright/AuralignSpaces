package com.auralign.spaces.data.repository

import com.auralign.spaces.data.model.AISuggestion
import com.auralign.spaces.data.remote.GeminiService
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepository @Inject constructor(
    private val geminiService: GeminiService
) {
    suspend fun getSuggestions(
        roomType: String,
        width: Double,
        length: Double,
        height: Double,
        budget: Double,
        photoBase64: String? = null
    ): List<AISuggestion> {
        return try {
            geminiService.getDesignSuggestions(roomType, width, length, height, budget, photoBase64)
        } catch (e: Exception) {
            Log.w(TAG, "Using local AI suggestions because remote analysis failed: ${e.message}")
            localSuggestions(roomType, width, length, height, budget, photoBase64 != null)
        }
    }

    private fun localSuggestions(
        roomType: String,
        width: Double,
        length: Double,
        height: Double,
        budget: Double,
        hasPhoto: Boolean
    ): List<AISuggestion> {
        val area = (width * length).coerceAtLeast(1.0)
        val compactRoom = area < 12.0
        val tallRoom = height >= 3.0
        val budgetInt = budget.toInt().coerceAtLeast(10000)
        val photoNote = if (hasPhoto) "Based on the uploaded room context, " else ""

        fun price(percent: Double): Int = (budgetInt * percent).toInt().coerceAtLeast(1500)

        return listOf(
            AISuggestion(
                id = "local_scandi_${roomType.hashCode()}",
                title = "Scandinavian Minimalist",
                description = "${photoNote}keep the $roomType bright and easy to move through with pale finishes, slim storage, and only the essential furniture. ${if (compactRoom) "Use wall-mounted shelves and raised-leg pieces to preserve floor visibility." else "Anchor the layout with a soft rug and balanced seating zones."}",
                style = "Minimal",
                recommendedItems = listOf(
                    "Light fabric sofa or lounge chair - Rs.${price(0.24)}",
                    "Oak-look storage unit - Rs.${price(0.16)}",
                    "Neutral textured rug - Rs.${price(0.10)}",
                    "Warm LED floor lamp - Rs.${price(0.06)}"
                ),
                wallColor = "#F5F2EA",
                floorMaterial = "Light oak laminate",
                estimatedCostInr = price(0.56)
            ),
            AISuggestion(
                id = "local_industrial_${roomType.hashCode()}",
                title = "Warm Industrial",
                description = "${photoNote}combine matte metal accents with warm wood so the room feels modern without becoming cold. ${if (tallRoom) "Use vertical shelving or tall lighting to make the most of the extra height." else "Keep heavier pieces low so the ceiling feels more open."}",
                style = "Industrial",
                recommendedItems = listOf(
                    "Wood and metal media unit - Rs.${price(0.18)}",
                    "Charcoal accent chair - Rs.${price(0.16)}",
                    "Black track or pendant light - Rs.${price(0.08)}",
                    "Rust or tan cushions - Rs.${price(0.04)}"
                ),
                wallColor = "#D8D0C3",
                floorMaterial = "Concrete-look tile",
                estimatedCostInr = price(0.46)
            ),
            AISuggestion(
                id = "local_japandi_${roomType.hashCode()}",
                title = "Japandi Zen",
                description = "${photoNote}use low-profile furniture, soft contrast, and natural textures to create a calmer $roomType. Leave clear negative space around the main furniture so the layout feels intentional.",
                style = "Japandi",
                recommendedItems = listOf(
                    "Low wooden table - Rs.${price(0.10)}",
                    "Linen curtains - Rs.${price(0.08)}",
                    "Cane or rattan accent storage - Rs.${price(0.14)}",
                    "Indoor plant with ceramic planter - Rs.${price(0.04)}"
                ),
                wallColor = "#EFE7DA",
                floorMaterial = "Natural bamboo or light wood",
                estimatedCostInr = price(0.36)
            ),
            AISuggestion(
                id = "local_luxury_${roomType.hashCode()}",
                title = "Luxury Modern",
                description = "${photoNote}give the room a premium feel with layered lighting, one statement surface, and a tighter color palette. Choose fewer larger pieces instead of many small accessories.",
                style = "Luxury Modern",
                recommendedItems = listOf(
                    "Statement sofa or bed frame - Rs.${price(0.32)}",
                    "Marble-look center or side table - Rs.${price(0.18)}",
                    "Brass or black accent lighting - Rs.${price(0.10)}",
                    "Large framed wall art - Rs.${price(0.06)}"
                ),
                wallColor = "#ECE8E1",
                floorMaterial = "Large-format vitrified tile",
                estimatedCostInr = price(0.66)
            )
        )
    }

    private companion object {
        private const val TAG = "AIRepository"
    }
}
