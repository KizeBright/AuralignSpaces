package com.auralign.spaces.data.repository

import android.util.Log
import com.auralign.spaces.data.model.SavedDesign
import com.auralign.spaces.data.model.FurnitureItem
import com.auralign.spaces.data.model.FloorTileMaterial
import com.auralign.spaces.data.model.WallPaintMaterial
import com.auralign.spaces.data.service.GitHubImageService
import com.auralign.spaces.domain.model.FurnitureCategory
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesignRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val githubImageService: GitHubImageService
) {
    private val userId get() = auth.currentUser?.uid ?: ""

    fun getSavedDesigns(): Flow<List<SavedDesign>> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(emptyList())
            return@callbackFlow
        }
        val subscription = firestore.collection("users").document(userId)
            .collection("designs")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val designs = snapshot?.toObjects(SavedDesign::class.java) ?: emptyList()
                trySend(designs)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveDesign(design: SavedDesign) {
        val docRef = if (design.id.isEmpty()) {
            firestore.collection("users").document(userId).collection("designs").document()
        } else {
            firestore.collection("users").document(userId).collection("designs").document(design.id)
        }
        val finalDesign = design.copy(id = docRef.id, userId = userId)
        docRef.set(finalDesign).await()
    }

    suspend fun saveDesignReturning(design: SavedDesign): SavedDesign {
        val docRef = if (design.id.isEmpty()) {
            firestore.collection("users").document(userId).collection("designs").document()
        } else {
            firestore.collection("users").document(userId).collection("designs").document(design.id)
        }
        val finalDesign = design.copy(id = docRef.id, userId = userId)
        docRef.set(finalDesign).await()
        return finalDesign
    }

    suspend fun uploadDesignThumbnail(designId: String, bitmap: android.graphics.Bitmap): String {
        if (userId.isEmpty()) return ""
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
        
        // Use GitHub CDN for thumbnails (FREE, no premium costs)
        return try {
            Log.d("DesignRepository", "Uploading thumbnail to GitHub CDN")
            githubImageService.uploadDesignThumbnail(designId, bytes)
        } catch (e: Exception) {
            Log.e("DesignRepository", "Failed to upload thumbnail to GitHub", e)
            ""  // Return empty string on failure - thumbnail is optional
        }
    }

    suspend fun updateDesignThumbnailUrl(designId: String, thumbnailUrl: String) {
        if (userId.isEmpty()) return
        firestore.collection("users").document(userId)
            .collection("designs").document(designId)
            .update(
                mapOf(
                    "thumbnailUrl" to thumbnailUrl,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            )
            .await()
    }

    suspend fun getSavedDesign(designId: String): SavedDesign? {
        if (userId.isEmpty()) return null
        return firestore.collection("users").document(userId)
            .collection("designs")
            .document(designId)
            .get()
            .await()
            .toObject(SavedDesign::class.java)
    }

    suspend fun deleteDesign(designId: String) {
        firestore.collection("users").document(userId).collection("designs").document(designId).delete().await()
    }

    suspend fun getFurnitureCatalog(): List<FurnitureItem> {
        val snap = firestore.collection("furniture_catalog").get().await()
        val items = snap.documents.mapNotNull { doc ->
            runCatching {
                val source = doc.toObject(FurnitureItem::class.java)
                fun stringField(vararg names: String): String =
                    names.firstNotNullOfOrNull { doc.getString(it)?.takeIf(String::isNotBlank) }.orEmpty()

                fun doubleField(vararg names: String): Double? =
                    names.firstNotNullOfOrNull { name ->
                        when (val value = doc.get(name)) {
                            is Number -> value.toDouble()
                            is String -> value.toDoubleOrNull()
                            else -> null
                        }
                    }

                val rawPrice = doc.get("price")
                    ?: doc.get("price Inr")
                    ?: doc.get("priceInr")
                    ?: doc.get("price_inr")
                val mappedPrice = when (rawPrice) {
                    is Number -> rawPrice.toDouble()
                    is String -> rawPrice.toDoubleOrNull()
                    else -> source?.price
                } ?: 0.0

                val category = try {
                    FurnitureCategory.valueOf(doc.getString("category") ?: "FURNITURE")
                } catch (_: Exception) {
                    FurnitureCategory.FURNITURE
                }

                FurnitureItem(
                    id = stringField("id").ifBlank { source?.id?.takeIf(String::isNotBlank) ?: doc.id },
                    name = stringField("name").ifBlank { source?.name.orEmpty() },
                    brand = stringField("brand").ifBlank { source?.brand.orEmpty() },
                    category = source?.category ?: category,
                    price = mappedPrice,
                    emoji = stringField("emoji").ifBlank { source?.emoji.orEmpty() },
                    modelId = stringField("modelId", "model Id").ifBlank { source?.modelId.orEmpty() },
                    thumbnailUrl = stringField("thumbnailUrl", "thumbnail Url").ifBlank { source?.thumbnailUrl.orEmpty() },
                    description = stringField("description").ifBlank { source?.description.orEmpty() },
                    widthM = doubleField("widthM", "width M") ?: source?.widthM ?: 0.0,
                    depthM = doubleField("depthM", "depth M") ?: source?.depthM ?: 0.0,
                    colorOptions = doc.get("colorOptions") as? List<String> ?: source?.colorOptions ?: emptyList(),
                    rating = doubleField("rating")?.toFloat() ?: source?.rating ?: 4.5f,
                    modelUrl = stringField("modelUrl", "model Url").ifBlank { source?.modelUrl.orEmpty() }
                )
            }.getOrNull()
        }
        val finalItems = if (items.isEmpty()) getMockCatalog() else items
        
        // Force inject the 3D model URL for the Leather Armchair if it was missed in Firestore
        return finalItems.map { item ->
            when {
                item.name == "Leather Armchair" && item.modelUrl.isEmpty() ->
                    item.copy(modelUrl = "https://raw.githubusercontent.com/mithra-n/auralign-models/main/chair-glb/source/chair%20GLB.glb")
                item.name.contains("Table", ignoreCase = true) && item.modelUrl.isEmpty() ->
                    item.copy(modelUrl = "https://raw.githubusercontent.com/mithra-n/auralign-models/main/table.glb")
                else -> item
            }
        }
    }

    suspend fun getWallPaintMaterials(): List<WallPaintMaterial> {
        val snap = firestore.collection("wall_paints").get().await()
        val items = snap.documents.mapNotNull { doc ->
            runCatching {
                val obj = doc.toObject(WallPaintMaterial::class.java) ?: return@runCatching null
                if (obj.id.isBlank()) obj.copy(id = doc.id) else obj
            }.getOrNull()
        }
        return if (items.isNotEmpty()) items else listOf(
            WallPaintMaterial(id = "paint_pearl", name = "Pearl White", hex = "#F5F5F0", price = 0.0, roughness = 0.95f, reflectance = 0.3f),
            WallPaintMaterial(id = "paint_ocean", name = "Ocean Blue", hex = "#1A2744", price = 0.0, roughness = 0.95f, reflectance = 0.3f),
            WallPaintMaterial(id = "paint_sage", name = "Sage Green", hex = "#6B8E6B", price = 0.0, roughness = 0.95f, reflectance = 0.3f),
        )
    }

    suspend fun getFloorTileMaterials(): List<FloorTileMaterial> {
        val snap = firestore.collection("floor_materials").get().await()
        val items = snap.documents.mapNotNull { doc ->
            runCatching {
                val obj = doc.toObject(FloorTileMaterial::class.java) ?: return@runCatching null
                if (obj.id.isBlank()) obj.copy(id = doc.id) else obj
            }.getOrNull()
        }
        return if (items.isNotEmpty()) items else listOf(
            FloorTileMaterial(id = "tile_oak", name = "Oak Plank", price = 0.0, textureUrl = "", roughness = 0.85f, reflectance = 0.4f, uvScale = 6.0f),
            FloorTileMaterial(id = "tile_stone", name = "Stone Grid", price = 0.0, textureUrl = "", roughness = 0.9f, reflectance = 0.35f, uvScale = 6.0f),
            FloorTileMaterial(id = "tile_marble", name = "Marble Check", price = 0.0, textureUrl = "", roughness = 0.75f, reflectance = 0.45f, uvScale = 6.0f),
        )
    }

    private fun getMockCatalog(): List<FurnitureItem> {
        return listOf(
            FurnitureItem("1", "Nordic Velvet Sofa", "AuraHome", FurnitureCategory.FURNITURE, 45000.0, "🛋️", "sofa_nordic", "", "A premium velvet sofa with minimalist legs.", 2.2, 0.9, listOf("#1A2744", "#2C2C2C")),
            FurnitureItem("2", "Oak Coffee Table", "AlignDesign", FurnitureCategory.FURNITURE, 12000.0, "🪵", "coffee_table", "", "Solid oak coffee table with a natural finish.", 1.2, 0.6, listOf("#3C2415")),
            FurnitureItem("3", "Industrial Floor Lamp", "Lumina", FurnitureCategory.LIGHTING, 8500.0, "💡", "floor_lamp", "", "Matte black floor lamp with adjustable head.", 0.4, 0.4, listOf("#000000")),
            FurnitureItem("4", "Persian Silk Rug", "Rugs & Co", FurnitureCategory.RUGS, 25000.0, "🧶", "rug_persian", "", "Hand-woven silk rug with intricate patterns.", 3.0, 2.0, listOf("#6B3A2A")),
            FurnitureItem("5", "Modern Bookshelf", "StoragePlus", FurnitureCategory.STORAGE, 18000.0, "📚", "bookshelf", "", "Floating shelf system for modern homes.", 1.0, 0.3, listOf("#FFFFFF", "#000000")),
            FurnitureItem("6", "Areca Palm", "Greenery", FurnitureCategory.PLANTS, 3500.0, "🪴", "plant_areca", "", "Low-maintenance indoor palm for a fresh look.", 0.6, 0.6, listOf("#059669")),
            FurnitureItem("7", "Abstract Canvas", "ArtAlign", FurnitureCategory.WALL_ART, 15000.0, "🖼️", "wall_art_1", "", "Large-scale abstract oil painting.", 1.5, 0.05, listOf("#7C3AED")),
            FurnitureItem("8", "Leather Armchair", "LuxurySit", FurnitureCategory.FURNITURE, 32000.0, "🪑", "armchair_leather", "", "Premium Italian leather armchair.", 0.9, 0.8, listOf("#3C2415", "#6B3A2A"), 4.5f, "https://raw.githubusercontent.com/mithra-n/auralign-models/main/chair-glb/source/chair%20GLB.glb"),
            FurnitureItem("9", "Modern Queen Bed", "AuraHome", FurnitureCategory.FURNITURE, 38000.0, "Bed", "modern_queen_bed", "", "Upholstered queen-size bed with a low modern frame.", 1.6, 2.1, listOf("#F5F5F0", "#3C2415")),
            FurnitureItem("10", "Freestanding Bathtub", "AquaNest", FurnitureCategory.FURNITURE, 52000.0, "Tub", "freestanding_bathtub", "", "Modern freestanding bathtub with smooth oval edges.", 0.8, 1.7, listOf("#FFFFFF")),
            // Floor tiles
            FurnitureItem("floor_oak", "Oak Plank Floor", "FloorCo", FurnitureCategory.FLOOR, 5000.0, "🏠", "floor_oak", "", "Solid oak plank flooring.", 3.0, 3.0, emptyList(), 0f, ""),
            FurnitureItem("floor_stone", "Stone Grid Floor", "FloorCo", FurnitureCategory.FLOOR, 6000.0, "🏠", "floor_stone", "", "Stone grid tile flooring.", 3.0, 3.0, emptyList(), 0f, ""),
            FurnitureItem("floor_marble", "Marble Check Floor", "FloorCo", FurnitureCategory.FLOOR, 7000.0, "🏠", "floor_marble", "", "Marble check pattern flooring.", 3.0, 3.0, emptyList(), 0f, ""),
            // Wall paints
            FurnitureItem("wall_pearl", "Pearl White Wall", "PaintCo", FurnitureCategory.WALL, 2000.0, "🏢", "wall_pearl", "", "Pearl white wall paint.", 2.5, 2.5, listOf("#F5F5F0"), 0f, ""),
            FurnitureItem("wall_ocean", "Ocean Blue Wall", "PaintCo", FurnitureCategory.WALL, 2500.0, "🏢", "wall_ocean", "", "Ocean blue wall paint.", 2.5, 2.5, listOf("#1A2744"), 0f, ""),
            FurnitureItem("wall_sage", "Sage Green Wall", "PaintCo", FurnitureCategory.WALL, 2200.0, "🏢", "wall_sage", "", "Sage green wall paint.", 2.5, 2.5, listOf("#6B8E6B"), 0f, "")
        )
    }
}
