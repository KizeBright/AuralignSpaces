package com.auralign.spaces.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.auralign.spaces.data.local.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val settingsManager: SettingsManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = settingsManager.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser
        if (user != null) {
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                try {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (doc.exists()) {
                        val name = doc.getString("name") ?: user.displayName ?: ""
                        val email = doc.getString("email") ?: user.email ?: ""
                        val photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString().orEmpty()
                        _state.update { it.copy(name = name, email = email, photoUrl = photoUrl, isLoading = false) }
                    } else {
                        // Edge case where document might not be created natively
                        _state.update {
                            it.copy(
                                name = user.displayName ?: "",
                                email = user.email ?: "",
                                photoUrl = user.photoUrl?.toString().orEmpty(),
                                isLoading = false
                            )
                        }
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
            }
        }
    }

    fun updateName(newName: String) {
        _state.update { it.copy(name = newName) }
    }

    fun toggleEdit() {
        _state.update { it.copy(isEditing = !it.isEditing) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDarkMode(enabled)
        }
    }

    fun saveProfile() {
        val user = auth.currentUser ?: return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                firestore.collection("users").document(user.uid)
                    .set(mapOf("name" to _state.value.name.trim()), SetOptions.merge())
                    .await()
                _state.update { it.copy(isEditing = false, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
