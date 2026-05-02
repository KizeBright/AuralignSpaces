package com.auralign.spaces.data.service

import android.util.Base64
import android.util.Log
import com.auralign.spaces.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Service to upload design thumbnails to GitHub CDN
 * Uses GitHub API to commit images to a public repository
 * 
 * GitHub Token setup:
 * 1. Go to https://github.com/settings/tokens
 * 2. Generate new token (classic) with 'public_repo' scope
 * 3. Add to local.properties: GITHUB_TOKEN=<your token>
 */
class GitHubImageService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    
    private val gson = Gson()
    
    // ⚠️ TODO: Configure these values before using
    private val githubToken = BuildConfig.GITHUB_TOKEN
    private val repoOwner = "mithra-n"
    private val repoName = "auralign-design-thumbnails"
    
    companion object {
        private const val BRANCH = "main"
    }
    
    data class GitHubCommitRequest(
        val message: String,
        val content: String,           // Base64 encoded image
        val branch: String = BRANCH
    )
    
    data class GitHubErrorResponse(
        val message: String,
        val errors: List<Map<String, Any>>? = null
    )
    
    /**
     * Upload a design thumbnail to GitHub CDN
     * 
     * @param designId Unique design identifier
     * @param imageBytes JPEG image bytes
     * @return CDN URL to the uploaded image, or empty string on failure
     */
    suspend fun uploadDesignThumbnail(designId: String, imageBytes: ByteArray): String = 
        withContext(Dispatchers.IO) {
            try {
                Log.d("GitHubImageService", "Starting thumbnail upload for design: $designId")
                
                // Validate token is configured
                if (githubToken.isBlank()) {
                    Log.e("GitHubImageService", "GitHub token not configured in BuildConfig")
                    return@withContext ""
                }
                
                // Convert image bytes to Base64
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                Log.d("GitHubImageService", "Image converted to Base64, size: ${base64Image.length} chars")
                
                val fileName = "$designId.jpg"
                val filePath = "thumbnails/$fileName"
                val cdnUrl = "https://raw.githubusercontent.com/$repoOwner/$repoName/$BRANCH/$filePath"
                
                // Build GitHub API request payload
                val request = GitHubCommitRequest(
                    message = "Add thumbnail for design $designId",
                    content = base64Image
                )
                
                val jsonPayload = gson.toJson(request)
                Log.d("GitHubImageService", "Sending to GitHub API: $filePath")
                
                // Create HTTP request
                val url = "https://api.github.com/repos/$repoOwner/$repoName/contents/$filePath"
                val httpRequest = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $githubToken")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .put(jsonPayload.toRequestBody("application/json".toMediaType()))
                    .build()
                
                // Execute request
                val response = okHttpClient.newCall(httpRequest).execute()
                
                if (response.isSuccessful) {
                    Log.d("GitHubImageService", "Upload successful! CDN URL: $cdnUrl")
                    return@withContext cdnUrl
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("GitHubImageService", "Upload failed: ${response.code} - $errorBody")
                    
                    // Log specific error details
                    try {
                        val errorResponse = gson.fromJson(errorBody, GitHubErrorResponse::class.java)
                        Log.e("GitHubImageService", "GitHub API Error: ${errorResponse.message}")
                    } catch (e: Exception) {
                        Log.e("GitHubImageService", "Could not parse error response")
                    }
                    
                    return@withContext ""
                }
            } catch (e: Exception) {
                Log.e("GitHubImageService", "Exception uploading thumbnail to GitHub", e)
                return@withContext ""
            }
        }
}
