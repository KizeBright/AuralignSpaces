package com.auralign.spaces.domain.usecase

import com.auralign.spaces.data.model.AISuggestion
import com.auralign.spaces.data.repository.AIRepository
import javax.inject.Inject

class GetAISuggestionsUseCase @Inject constructor(
    private val repository: AIRepository
) {
    suspend fun execute(
        roomType: String,
        width: Double,
        length: Double,
        height: Double,
        budget: Double,
        photoBase64: String? = null
    ): List<AISuggestion> {
        return repository.getSuggestions(roomType, width, length, height, budget, photoBase64)
    }
}
