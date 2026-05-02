package com.auralign.spaces.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralign.spaces.data.model.AISuggestion
import com.auralign.spaces.data.model.RoomConfig
import com.auralign.spaces.data.repository.AIRepository
import com.auralign.spaces.domain.model.RoomType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val selectedRoomType: RoomType = RoomType.LIVING,
    val width: Double = RoomType.LIVING.defaultW,
    val length: Double = RoomType.LIVING.defaultL,
    val height: Double = RoomType.LIVING.defaultH,
    val budget: Double = 50000.0,
    val photoUrl: String? = null,
    val isCustomizing: Boolean = false,
    // AI analysis state
    val isAnalyzing: Boolean = false,
    val aiSuggestions: List<AISuggestion> = emptyList(),
    val analysisError: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun selectRoomType(roomType: RoomType) {
        _state.update { it.copy(
            selectedRoomType = roomType,
            width = roomType.defaultW,
            length = roomType.defaultL,
            height = roomType.defaultH
        ) }
    }

    fun updateDimensions(w: Double? = null, l: Double? = null, h: Double? = null) {
        _state.update { it.copy(
            width = w ?: it.width,
            length = l ?: it.length,
            height = h ?: it.height
        ) }
    }

    fun updateBudget(budget: Double) {
        _state.update { it.copy(budget = budget) }
    }

    fun setPhotoUrl(url: String?) {
        // Just store the photo — analysis is triggered manually by the user
        _state.update { it.copy(photoUrl = url, aiSuggestions = emptyList(), analysisError = null) }
    }

    fun analyzeRoom() {
        val url = _state.value.photoUrl ?: return
        analyzePhoto(url)
    }

    fun toggleCustomizing() {
        _state.update { it.copy(isCustomizing = !it.isCustomizing) }
    }

    private fun analyzePhoto(uriString: String) {
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, analysisError = null) }
            try {
                val base64 = uriToBase64(uriString)
                val s = _state.value
                val suggestions = aiRepository.getSuggestions(
                    roomType = s.selectedRoomType.label,
                    width = s.width,
                    length = s.length,
                    height = s.height,
                    budget = s.budget,
                    photoBase64 = base64
                )
                _state.update { it.copy(aiSuggestions = suggestions, isAnalyzing = false) }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("429") == true || e.message?.contains("RATE_LIMITED") == true ->
                        "AI quota limit reached. Please wait a minute and try again."
                    e.message?.contains("404") == true ->
                        "AI model not available. Please try again later."
                    else -> "Analysis failed: ${e.message?.take(100)}"
                }
                _state.update { it.copy(isAnalyzing = false, analysisError = errorMsg) }
            }
        }
    }

    private fun uriToBase64(uriString: String): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Decode original bitmap
            val original = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (original == null) return null

            // Scale down to max 800px on longest side to keep payload small
            val maxDim = 800
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaledW = (original.width * scale).toInt()
            val scaledH = (original.height * scale).toInt()
            val scaled = android.graphics.Bitmap.createScaledBitmap(original, scaledW, scaledH, true)

            // Compress to JPEG at 75% quality
            val outputStream = java.io.ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
            val bytes = outputStream.toByteArray()

            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Image encoding failed: ${e.message}")
            null
        }
    }

    fun getRoomConfig(): RoomConfig {
        val s = _state.value
        return RoomConfig(
            type = s.selectedRoomType.label,
            width = s.width,
            length = s.length,
            height = s.height,
            budget = s.budget,
            photoUrl = s.photoUrl
        )
    }
}
