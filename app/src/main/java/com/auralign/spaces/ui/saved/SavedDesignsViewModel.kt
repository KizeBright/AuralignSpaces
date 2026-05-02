package com.auralign.spaces.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralign.spaces.data.model.SavedDesign
import com.auralign.spaces.data.repository.DesignRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedDesignsViewModel @Inject constructor(
    private val repository: DesignRepository
) : ViewModel() {

    val savedDesigns: StateFlow<List<SavedDesign>> = repository.getSavedDesigns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteDesign(designId: String) {
        viewModelScope.launch {
            repository.deleteDesign(designId)
        }
    }
}
