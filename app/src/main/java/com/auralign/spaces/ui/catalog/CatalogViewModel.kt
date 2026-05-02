package com.auralign.spaces.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralign.spaces.data.model.FurnitureItem
import com.auralign.spaces.data.repository.DesignRepository
import com.auralign.spaces.domain.model.FurnitureCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogState(
    val items: List<FurnitureItem> = emptyList(),
    val filteredItems: List<FurnitureItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: FurnitureCategory? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val repository: DesignRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val items = repository.getFurnitureCatalog()
                _state.update { it.copy(items = items, filteredItems = items, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        filterItems()
    }

    fun onCategorySelect(category: FurnitureCategory?) {
        _state.update { it.copy(selectedCategory = category) }
        filterItems()
    }

    private fun filterItems() {
        _state.update { s ->
            val filtered = s.items.filter { item ->
                (s.searchQuery.isEmpty() || item.name.contains(s.searchQuery, ignoreCase = true) || item.brand.contains(s.searchQuery, ignoreCase = true)) &&
                (s.selectedCategory == null || item.category == s.selectedCategory)
            }
            s.copy(filteredItems = filtered)
        }
    }
}
