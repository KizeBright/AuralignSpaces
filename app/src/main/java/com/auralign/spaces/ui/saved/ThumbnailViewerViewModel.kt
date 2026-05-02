package com.auralign.spaces.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralign.spaces.data.model.SavedDesign
import com.auralign.spaces.data.repository.DesignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThumbnailViewerViewModel @Inject constructor(
    private val designRepository: DesignRepository
) : ViewModel() {

    private val _design = MutableStateFlow<SavedDesign?>(null)
    val design: StateFlow<SavedDesign?> = _design

    fun loadDesign(designId: String) {
        viewModelScope.launch {
            try {
                val loadedDesign = designRepository.getSavedDesign(designId)
                _design.value = loadedDesign
            } catch (e: Exception) {
                _design.value = null
            }
        }
    }
}