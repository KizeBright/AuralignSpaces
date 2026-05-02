package com.auralign.spaces.domain.usecase

import com.auralign.spaces.data.model.User
import com.auralign.spaces.data.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend fun execute(email: String, password: String): User {
        return repository.signInWithEmail(email, password)
    }
}
