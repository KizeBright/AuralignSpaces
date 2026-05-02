package com.auralign.spaces.domain.usecase

import com.auralign.spaces.data.model.SavedDesign
import com.auralign.spaces.data.repository.DesignRepository
import javax.inject.Inject

class SaveDesignUseCase @Inject constructor(
    private val repository: DesignRepository
) {
    suspend fun execute(design: SavedDesign) {
        repository.saveDesign(design)
    }
}
