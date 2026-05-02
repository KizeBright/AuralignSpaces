package com.auralign.spaces.data.repository

import com.auralign.spaces.data.model.AISuggestion
import com.auralign.spaces.data.remote.GeminiService
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
        return geminiService.getDesignSuggestions(roomType, width, length, height, budget, photoBase64)
    }
}
