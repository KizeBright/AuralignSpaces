# AuralignSpaces: Migrate from Firebase Storage to GitHub CDN (FREE)

## Problem Summary
1. âœ… Budget calculation working correctly
2. âŒ Saved design thumbnails need storage (currently Firebase Storage = PREMIUM)
3. âŒ Placed items not showing when loading saved designs (Firestore read issue)

---

## SOLUTION: Use GitHub + Raw CDN for Thumbnails

### Why GitHub CDN?
- **FREE forever** (no premium costs)
- **Unlimited storage** (within GitHub repo size limits)
- **Fast CDN** via `raw.githubusercontent.com`
- **Version control** - track thumbnail changes
- **No API rate limits** for your use case

### Comparison with Other Options:
| Option | Free Tier | Ease | Setup Time |
|--------|-----------|------|-----------|
| **GitHub CDN** | âœ… Unlimited | â­â­ Easy | 20 min |
| ImgBB | âœ… 5GB/month | â­â­â­ Simple | 10 min |
| Cloudinary | âœ… 25GB/month | â­â­â­ Simple | 15 min |
| AWS S3 | âŒ $$ (pay-per-use) | â­ Complex | 30 min |

---

# STEP-BY-STEP MIGRATION GUIDE

## Step 1: Create GitHub Repository for Thumbnails

### 1.1 Create a new public repository
1. Go to https://github.com/new
2. Repository name: `auralign-design-thumbnails`
3. Description: `Thumbnail storage for AuralignSpaces app`
4. âœ… **Public** (required for CDN access without auth)
5. âœ… Add README
6. âœ… Add .gitignore (select "Python" or generic)
7. Click **Create repository**

### 1.2 Clone the repository locally
```bash
git clone https://github.com/YOUR_USERNAME/auralign-design-thumbnails.git
cd auralign-design-thumbnails
```

### 1.3 Create folder structure
```bash
mkdir -p thumbnails
cd thumbnails
touch .gitkeep
git add .gitkeep
git commit -m "Initial commit: thumbnail structure"
git push origin main
```

---

## Step 2: Update Android App Code

### 2.1 Create GitHub Upload Service
Create file: `app/src/main/java/com/auralign/spaces/data/service/GitHubImageService.kt`

```kotlin
package com.auralign.spaces.data.service

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GitHubImageService(private val okHttpClient: OkHttpClient) {

    private val gson = Gson()
    private val GITHUB_TOKEN = "YOUR_GITHUB_TOKEN_HERE"  // See Step 3
    private val REPO_OWNER = "YOUR_USERNAME"              // Your GitHub username
    private val REPO_NAME = "auralign-design-thumbnails"   // Repo name
    private val BRANCH = "main"

    data class GitHubCommitRequest(
        val message: String,
        val content: String,
        val branch: String = BRANCH
    )

    suspend fun uploadDesignThumbnail(designId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
            val fileName = "$designId.jpg"
            val filePath = "thumbnails/$fileName"

            val request = GitHubCommitRequest(
                message = "Add thumbnail for design $designId",
                content = base64Image
            )

            val url = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/contents/$filePath"

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $GITHUB_TOKEN")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .put(
                    gson.toJson(request).toRequestBody("application/json".toMediaType())
                )
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()

            if (response.isSuccessful) {
                // Return the CDN URL for the image
                return@withContext "https://raw.githubusercontent.com/$REPO_OWNER/$REPO_NAME/$BRANCH/thumbnails/$fileName"
            } else {
                throw Exception("GitHub upload failed: ${response.code} - ${response.body?.string()}")
            }
        } catch (e: Exception) {
            throw Exception("Error uploading thumbnail to GitHub: ${e.message}", e)
        }
    }
}
```

### 2.2 Update AppModule (Dependency Injection)
**File**: `app/src/main/java/com/auralign/spaces/di/AppModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // ... existing code ...

    @Provides
    @Singleton
    fun provideGitHubImageService(okHttpClient: OkHttpClient): GitHubImageService {
        return GitHubImageService(okHttpClient)
    }
}
```

### 2.3 Update DesignRepository to Use GitHub
**File**: `app/src/main/java/com/auralign/spaces/data/repository/DesignRepository.kt`

Replace:
```kotlin
suspend fun uploadDesignThumbnail(designId: String, bitmap: android.graphics.Bitmap): String {
    if (userId.isEmpty()) return ""
    val bytes = ByteArrayOutputStream().use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
    val ref = storage.reference
        .child("users")
        .child(userId)
        .child("design_thumbnails")
        .child("$designId.jpg")

    ref.putBytes(bytes).await()
    return ref.downloadUrl.await().toString()
}
```

With:
```kotlin
suspend fun uploadDesignThumbnail(designId: String, bitmap: android.graphics.Bitmap): String {
    if (userId.isEmpty()) return ""
    val bytes = ByteArrayOutputStream().use { out ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
    return try {
        githubImageService.uploadDesignThumbnail(designId, bytes)
    } catch (e: Exception) {
        Log.e("DesignRepository", "Failed to upload thumbnail to GitHub", e)
        ""  // Return empty string on failure - thumbnail is optional
    }
}
```

Add to DesignRepository constructor:
```kotlin
private val githubImageService: GitHubImageService
```

---

## Step 3: Create GitHub Personal Access Token (PAT)

### 3.1 Generate Token
1. Go to https://github.com/settings/tokens
2. Click **Generate new token** â†’ **Generate new token (classic)**
3. Token name: `AuralignSpaces`
4. Expiration: **90 days** (auto-renewable, recommended)
5. Select scopes:
   - âœ… `public_repo` (access public repositories)
   - âŒ No other scopes needed
6. Click **Generate token**
7. **COPY the token immediately** (you won't see it again)

### 3.2 Add Token to Your Android App

**OPTION A: Secure (Recommended - Use BuildConfig)**
1. Create `local.properties` in project root:
```
github.token=ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```
2. Add to `build.gradle.kts` (app level):
```kotlin
android {
    ...
    buildTypes {
        release {
            buildConfigField("String", "GITHUB_TOKEN", "\"${project.property("github.token")}\"")
        }
        debug {
            buildConfigField("String", "GITHUB_TOKEN", "\"${project.property("github.token")}\"")
        }
    }
}
```
3. Update GitHubImageService.kt:
```kotlin
private val GITHUB_TOKEN = BuildConfig.GITHUB_TOKEN
```

**OPTION B: Firebase Remote Config (Most Secure)**
1. Go to Firebase Console â†’ Project Settings â†’ Remote Config
2. Add parameter:
   ```
   github_token = ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
3. Update GitHubImageService to fetch from Remote Config instead

---

## Step 4: Fix Placed Items Not Loading (Firestore Issue)

### 4.1 Debug the Issue
Update `DesignerViewModel.kt` loadDesign function with logging:

```kotlin
fun loadDesign(design: SavedDesign) {
    viewModelScope.launch {
        try {
            Log.d("DesignerVM", "Loading design: ${design.id}, placedItems count: ${design.placedItems.size}")

            val catalog = designRepository.getFurnitureCatalog()
            Log.d("DesignerVM", "Catalog loaded: ${catalog.size} items")

            val paints = designRepository.getWallPaintMaterials()
            val floors = designRepository.getFloorTileMaterials()

            val placed = design.placedItems.mapNotNull { dto ->
                Log.d("DesignerVM", "Looking for item with ID: ${dto.itemId}")
                catalog.find { it.id == dto.itemId }?.let { item ->
                    Log.d("DesignerVM", "Found item: ${item.name}")
                    val compatPlaced = dto.isPlaced || dto.posX != 0f || dto.posY != 0f || dto.posZ != 0f
                    PlacedObject(
                        instanceId = dto.instanceId,
                        item = item,
                        isPlaced = compatPlaced,
                        posX = dto.posX, posY = dto.posY, posZ = dto.posZ,
                        rotQx = dto.rotQx, rotQy = dto.rotQy, rotQz = dto.rotQz, rotQw = dto.rotQw,
                        rotationDeg = dto.rotationDeg,
                        scale = dto.scale,
                        selectedColorHex = dto.selectedColorHex
                    )
                } ?: run {
                    Log.w("DesignerVM", "Item not found in catalog: ${dto.itemId}")
                    null
                }
            }

            Log.d("DesignerVM", "Placed items restored: ${placed.size} items")

            // ... rest of the function ...
        } catch (e: Exception) {
            Log.e("DesignerVM", "Error loading design", e)
        }
    }
}
```

### 4.2 Verify Firestore Has Furniture Catalog
Check your Firestore console:
1. Go to Firebase Console â†’ Firestore Database
2. Check collection: `furniture_catalog`
3. Verify documents have matching IDs with the mock data (e.g., "8" for Leather Armchair)
4. If empty, manually add furniture items or populate from your mock catalog

---

## Step 5: Deploy Changes (Git Workflow)

### 5.1 Commit Code Changes
```bash
cd /path/to/AuralignSpaces
git add app/src/main/java/com/auralign/spaces/data/service/GitHubImageService.kt
git add app/src/main/java/com/auralign/spaces/data/repository/DesignRepository.kt
git add app/src/main/java/com/auralign/spaces/di/AppModule.kt
git add app/src/main/java/com/auralign/spaces/ui/designer/DesignerViewModel.kt
git commit -m "feat: migrate thumbnail storage from Firebase Storage to GitHub CDN

- Replace Firebase Storage uploads with GitHub API
- Add GitHubImageService for image uploads
- Update DesignRepository to use new service
- Add debug logging to fix placed items loading issue
- Eliminates premium Firebase Storage costs"

git push origin main
```

### 5.2 Build & Test Android App
```bash
./gradlew clean build
```

---

## Step 6: Testing Checklist

- [ ] Create a new design with furniture items
- [ ] Place items in AR (at least 2-3 items)
- [ ] Place floor and wall materials
- [ ] Click **Save Design**
- [ ] Check that thumbnail is uploaded to GitHub repo
  - Go to https://github.com/YOUR_USERNAME/auralign-design-thumbnails/tree/main/thumbnails
  - Should see files like: `design_xxx.jpg`
- [ ] Go to "Saved Designs" screen
- [ ] Click on saved design
- [ ] âœ… Verify placed items appear in AR
- [ ] âœ… Verify budget spent shows correctly
- [ ] âœ… Edit design and verify can modify items

---

## Alternative: Use ImgBB Instead (If GitHub Doesn't Work)

**If you prefer NOT to use GitHub, here's ImgBB setup:**

### ImgBB Service
```kotlin
class ImgBBImageService(private val okHttpClient: OkHttpClient) {

    private val IMGBB_API_KEY = "YOUR_IMGBB_API_KEY"  // Get from https://api.imgbb.com/

    suspend fun uploadDesignThumbnail(designId: String, imageBytes: ByteArray): String = withContext(Dispatchers.IO) {
        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", imageBytes)
            .addFormDataPart("key", IMGBB_API_KEY)

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(multipartBuilder.build())
            .build()

        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext ""

        // Parse JSON and extract image URL
        val jsonObject = JSONObject(responseBody)
        return@withContext jsonObject.getJSONObject("data").getString("url")
    }
}
```

**Pros:**
- Simpler API
- Better for images
- 5GB/month free tier

**Cons:**
- Need API key
- Monthly quota limit

---

## Summary of Changes

| Component | Change | Reason |
|-----------|--------|--------|
| Storage Method | Firebase â†’ GitHub CDN | Eliminate premium costs |
| Upload Mechanism | Storage.reference â†’ GitHub API | Use free alternative |
| Logging | Added comprehensive logs | Debug placed items issue |
| Dependency | Add GitHubImageService | Handle uploads |
| Config | Add GITHUB_TOKEN | Authenticate API calls |

---

## Cost Comparison

### Before (Firebase Storage)
- **Free Tier**: 5GB/month, then $0.18/GB
- **Your Usage**: If 100 designs Ã— 2MB = 200MB, still free
- **But**: Premium lock for future scaling

### After (GitHub CDN)
- **Cost**: $0 (included in GitHub)
- **Storage**: Unlimited (within repo size limits)
- **Scalability**: No premium upgrade needed

**Total Savings**: ~$0-$20/month depending on usage, plus future-proof âœ…

---

## Questions?

If you hit issues:
1. Check Android Logcat for errors with tag `DesignerVM` or `GitHubImageService`
2. Verify GITHUB_TOKEN is set correctly in BuildConfig
3. Ensure GitHub repo is PUBLIC (not private)
4. Check Firestore `furniture_catalog` has data
