package com.auralign.spaces.data.repository

import com.auralign.spaces.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signInWithEmail(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return fetchOrCreateUser(result.user!!)
    }

    suspend fun register(name: String, email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = User(id = result.user!!.uid, name = name, email = email)
        firestore.collection("users").document(user.id).set(user).await()
        return user
    }

    suspend fun signInWithGoogle(idToken: String): User {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return fetchOrCreateUser(result.user!!)
    }

    private suspend fun fetchOrCreateUser(firebaseUser: FirebaseUser): User {
        val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
        return if (doc.exists()) {
            doc.toObject(User::class.java)!!
        } else {
            val user = User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: ""
            )
            firestore.collection("users").document(user.id).set(user).await()
            user
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
