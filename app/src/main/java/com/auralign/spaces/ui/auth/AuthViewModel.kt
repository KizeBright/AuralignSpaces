package com.auralign.spaces.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralign.spaces.data.model.User
import com.auralign.spaces.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isSignInTab = MutableStateFlow(true)
    val isSignInTab: StateFlow<Boolean> = _isSignInTab.asStateFlow()

    fun setTab(isSignIn: Boolean) {
        _isSignInTab.value = isSignIn
    }

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = repository.signInWithEmail(email, password)
                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Sign in error", e)
                _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = repository.signInWithGoogle(idToken)
                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign-In failed")
            }
        }
    }

    fun onGoogleSignInFailed(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val user = repository.register(name, email, password)
                _authState.value = AuthState.Success(user)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Register error", e)
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _authState.value = AuthState.Idle
    }
}
