package com.auralign.spaces.ui.designer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralign.spaces.data.model.*
import com.auralign.spaces.data.repository.AIRepository
import com.auralign.spaces.data.repository.DesignRepository
import com.auralign.spaces.domain.model.FurnitureCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class DesignerState(
    val roomConfig: RoomConfig = RoomConfig(),
    val placedItems: List<PlacedObject> = emptyList(),
    val catalog: List<FurnitureItem> = emptyList(),
    val wallPaintOptions: List<WallPaintMaterial> = emptyList(),
    val floorMaterialOptions: List<FloorTileMaterial> = emptyList(),
    val wallPaint: WallPaintMaterial = WallPaintMaterial(id = "paint_pearl", name = "Pearl White", hex = "#F5F5F0"),
    val floorMaterial: FloorTileMaterial = FloorTileMaterial(id = "tile_oak", name = "Oak Plank"),
    val activeCategory: FurnitureCategory = FurnitureCategory.FURNITURE,
    val selectedInstanceId: String? = null,
    val pendingPlacementId: String? = null,
    val pendingFloorPlacement: Boolean = false,
    val pendingWallPlacement: Boolean = false,
    val floorPlaced: Boolean = false,
    val wallPlaced: Boolean = false,
    val isSaving: Boolean = false,
    val screenshotRequestNonce: Long = 0L,
    val pendingThumbnailDesignId: String? = null,
    val showSummary: Boolean = false,
    val showAI: Boolean = false,
    val aiSuggestions: List<AISuggestion> = emptyList(),
    val isAiLoading: Boolean = false,
    val showCustomizer: Boolean = false,
    val customizerTab: Int = 0,
    val saveSuccess: Boolean = false
) {
    val visibleCatalog: List<FurnitureItem> get() = catalog.filter { item ->
        item.isVisibleInRoom(roomConfig.type)
    }
    val totalSpent: Double get() = placedItems
        .filter { it.isPlaced && it.instanceId !in listOf("floor", "wall") }
        .sumOf { it.item.price } +
        (if (floorPlaced) floorMaterial.price else 0.0) +
        (if (wallPlaced) wallPaint.price else 0.0)
    val budgetRemaining: Double get() = (roomConfig.budget - totalSpent).coerceAtLeast(0.0)
    val budgetPercent: Float get() = if (roomConfig.budget > 0) (totalSpent / roomConfig.budget).toFloat().coerceIn(0f, 1f) else 0f
    val isOverBudget: Boolean get() = totalSpent > roomConfig.budget
    val selectedItem: PlacedObject? get() = placedItems.find { it.instanceId == selectedInstanceId }
    val anyPendingPlacement: Boolean get() = pendingPlacementId != null || pendingFloorPlacement || pendingWallPlacement
}

private fun FurnitureItem.isVisibleInRoom(roomType: String): Boolean {
    val room = roomType.lowercase(Locale.ROOT)
    val searchable = listOf(id, name, modelId, description)
        .joinToString(" ")
        .lowercase(Locale.ROOT)

    val isChair = searchable.contains("chair") || searchable.contains("armchair")
    val isTable = searchable.contains("table")
    val isBed = searchable.contains("bed")
    val isBathTub = searchable.contains("bathtub") ||
        searchable.contains("bath tub") ||
        searchable.contains("tub")

    return when {
        isChair -> true
        isTable -> room.contains("living") || room.contains("office") || room.contains("bedroom")
        isBed -> room.contains("bedroom")
        isBathTub -> room.contains("bathroom")
        else -> true
    }
}

@HiltViewModel
class DesignerViewModel @Inject constructor(
    private val designRepository: DesignRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DesignerState())
    val state: StateFlow<DesignerState> = _state.asStateFlow()

    init {
        loadCatalog()
        loadMaterials()
    }

    private fun loadMaterials() {
        viewModelScope.launch {
            try {
                val paints = designRepository.getWallPaintMaterials()
                val floors = designRepository.getFloorTileMaterials()
                _state.update { s ->
                    val wall = paints.firstOrNull { it.id == s.wallPaint.id } ?: paints.firstOrNull() ?: s.wallPaint
                    val floor = floors.firstOrNull { it.id == s.floorMaterial.id } ?: floors.firstOrNull() ?: s.floorMaterial
                    s.copy(
                        wallPaintOptions = paints,
                        floorMaterialOptions = floors,
                        wallPaint = wall,
                        floorMaterial = floor
                    )
                }
            } catch (_: Exception) {
                // Keep defaults if DB is unavailable.
            }
        }
    }

    fun setRoomConfig(config: RoomConfig) {
        _state.update { it.copy(roomConfig = config) }
        config.designId?.let { id ->
            viewModelScope.launch {
                try {
                    val design = designRepository.getSavedDesign(id)
                    if (design != null) {
                        loadDesign(design)
                    } else {
                        val allDesigns = designRepository.getSavedDesigns().firstOrNull() ?: return@launch
                        val fallbackDesign = allDesigns.find { it.id == id } ?: return@launch
                        loadDesign(fallbackDesign)
                    }
                } catch (e: Exception) { /* silent fail */ }
            }
        }
    }

    fun loadDesign(design: SavedDesign) {
        viewModelScope.launch {
            try {
                Log.d("DesignerVM", "🔄 Loading design: ${design.id}")
                Log.d("DesignerVM", "📦 Saved placedItems count: ${design.placedItems.size}")
                
                val catalog = designRepository.getFurnitureCatalog()
                Log.d("DesignerVM", "📚 Furniture catalog loaded: ${catalog.size} items")
                catalog.forEachIndexed { idx, item ->
                    Log.d("DesignerVM", "  [$idx] ID=${item.id}, Name=${item.name}, Price=${item.price}")
                }
                
                val paints = designRepository.getWallPaintMaterials()
                Log.d("DesignerVM", "🎨 Wall paints loaded: ${paints.size} options")
                
                val floors = designRepository.getFloorTileMaterials()
                Log.d("DesignerVM", "🪵 Floor materials loaded: ${floors.size} options")
                
                val placed = design.placedItems.mapNotNull { dto ->
                    Log.d("DesignerVM", "🔍 Looking for item with ID: ${dto.itemId}")
                    
                    val foundItem = catalog.find { it.id == dto.itemId }
                    if (foundItem != null) {
                        Log.d("DesignerVM", "✅ Found item: ${foundItem.name} (${foundItem.category})")
                        val compatPlaced = dto.isPlaced || dto.posX != 0f || dto.posY != 0f || dto.posZ != 0f
                        PlacedObject(
                            instanceId = dto.instanceId,
                            item = foundItem,
                            isPlaced = compatPlaced,
                            posX = dto.posX, posY = dto.posY, posZ = dto.posZ,
                            rotQx = dto.rotQx, rotQy = dto.rotQy, rotQz = dto.rotQz, rotQw = dto.rotQw,
                            rotationDeg = dto.rotationDeg,
                            scale = dto.scale,
                            selectedColorHex = dto.selectedColorHex
                        )
                    } else {
                        Log.w("DesignerVM", "❌ Item NOT found in catalog: ${dto.itemId}")
                        Log.w("DesignerVM", "   Available IDs: ${catalog.map { it.id }}")
                        null
                    }
                }
                
                Log.d("DesignerVM", "✨ Placed items restored: ${placed.size} items")
                placed.forEachIndexed { idx, obj ->
                    Log.d("DesignerVM", "  [$idx] ${obj.item.name} - isPlaced=${obj.isPlaced}, pos=(${obj.posX}, ${obj.posY}, ${obj.posZ})")
                }
                
                val wallById = paints.firstOrNull { it.id == design.wallPaintId }
                val wallByHex = paints.firstOrNull { it.hex.equals(design.wallColorHex, ignoreCase = true) }
                val wall = wallById ?: wallByHex ?: paints.firstOrNull() ?: _state.value.wallPaint

                val floor = floors.firstOrNull { it.id == design.floorMaterialId } ?: floors.firstOrNull() ?: _state.value.floorMaterial
                
                _state.update { it.copy(
                    roomConfig = RoomConfig(design.roomType, design.widthM, design.lengthM, design.heightM, design.budget, designId = design.id),
                    placedItems = placed,
                    wallPaintOptions = paints,
                    floorMaterialOptions = floors,
                    wallPaint = wall,
                    floorMaterial = floor,
                    floorPlaced = design.floorMaterialId.isNotEmpty(),
                    wallPlaced = design.wallPaintId.isNotEmpty() || design.wallColorHex.isNotEmpty()
                )}
                Log.d("DesignerVM", "✅ Design loaded successfully!")
            } catch (e: Exception) {
                Log.e("DesignerVM", "❌ Error loading design", e)
            }
        }
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            try {
                val items = designRepository.getFurnitureCatalog()
                _state.update { it.copy(catalog = items) }
            } catch (e: Exception) { }
        }
    }

    fun selectCategory(category: FurnitureCategory) {
        _state.update { it.copy(activeCategory = category, showCustomizer = false) }
    }

    fun toggleCustomizer(show: Boolean, tab: Int = 0) {
        _state.update { it.copy(showCustomizer = show, customizerTab = tab, selectedInstanceId = null) }
    }

    fun toggleAI(show: Boolean) {
        _state.update { it.copy(showAI = show) }
    }

    fun addItem(item: FurnitureItem) {
        if (_state.value.anyPendingPlacement) return
        if (_state.value.budgetRemaining < item.price) return
        when (item.category) {
            FurnitureCategory.FLOOR -> {
                val materialId = item.id.replace("floor_", "tile_")
                val material = _state.value.floorMaterialOptions.find { it.id == materialId } ?: _state.value.floorMaterial
                val floorItem = PlacedObject(
                    instanceId = "floor",
                    item = item,
                    isPlaced = false,
                    posX = 0f, posY = 0f, posZ = 0f,
                    rotQw = 1f
                )
                _state.update {
                    it.copy(
                        placedItems = it.placedItems.filter { p -> p.instanceId != "floor" } + floorItem,
                        floorMaterial = material,
                        selectedInstanceId = "floor",
                        pendingFloorPlacement = true,
                        showCustomizer = false
                    )
                }
            }
            FurnitureCategory.WALL -> {
                val materialId = item.id.replace("wall_", "paint_")
                val material = _state.value.wallPaintOptions.find { it.id == materialId } ?: _state.value.wallPaint
                val wallItem = PlacedObject(
                    instanceId = "wall",
                    item = item,
                    isPlaced = false,
                    posX = 0f, posY = 0f, posZ = 0f,
                    rotQw = 1f
                )
                _state.update {
                    it.copy(
                        placedItems = it.placedItems.filter { p -> p.instanceId != "wall" } + wallItem,
                        wallPaint = material,
                        selectedInstanceId = "wall",
                        pendingWallPlacement = true,
                        showCustomizer = false
                    )
                }
            }
            else -> {
                val placed = PlacedObject(
                    instanceId = UUID.randomUUID().toString(),
                    item = item,
                    isPlaced = false,
                    posX = 0f, posY = 0f, posZ = 0f,
                    rotQw = 1f
                )
                _state.update {
                    it.copy(
                        placedItems = it.placedItems + placed,
                        selectedInstanceId = placed.instanceId,
                        pendingPlacementId = placed.instanceId,
                        showCustomizer = false
                    )
                }
            }
        }
    }

    fun removeItem(instanceId: String) {
        _state.update { it.copy(
            placedItems = it.placedItems.filter { item -> item.instanceId != instanceId },
            selectedInstanceId = if (it.selectedInstanceId == instanceId) null else it.selectedInstanceId,
            pendingPlacementId = if (it.pendingPlacementId == instanceId) null else it.pendingPlacementId
        )}
    }

    fun selectItem(instanceId: String?) {
        _state.update {
            val selected = instanceId?.let { id -> it.placedItems.find { p -> p.instanceId == id } }
            it.copy(
                selectedInstanceId = instanceId,
                pendingPlacementId = if (selected != null && !selected.isPlaced) instanceId else null
            )
        }
    }

    fun moveItemToPlane(instanceId: String) {
        _state.update { s ->
            val selected = s.placedItems.find { it.instanceId == instanceId } ?: return@update s
            if (selected.instanceId == "floor" || selected.instanceId == "wall") {
                s
            } else {
                s.copy(
                    selectedInstanceId = instanceId,
                    pendingPlacementId = instanceId,
                    pendingFloorPlacement = false,
                    pendingWallPlacement = false
                )
            }
        }
    }

    fun moveItem(instanceId: String, dx: Float, dy: Float) {
        _state.update { s ->
            s.copy(placedItems = s.placedItems.map {
                if (it.instanceId == instanceId) it.copy(posX = it.posX + dx, posZ = it.posZ + dy) else it
            })
        }
    }

    fun updateItemTransform(instanceId: String, rotation: Float? = null, scale: Float? = null) {
        _state.update { s ->
            s.copy(placedItems = s.placedItems.map {
                if (it.instanceId == instanceId) {
                    it.copy(
                        rotationDeg = rotation ?: it.rotationDeg,
                        scale = scale ?: it.scale
                    )
                } else it
            })
        }
    }

    fun placeItemFromPose(
        instanceId: String,
        posX: Float,
        posY: Float,
        posZ: Float,
        rotQx: Float,
        rotQy: Float,
        rotQz: Float,
        rotQw: Float,
    ) {
        _state.update { s ->
            val isFloor = instanceId == "floor"
            val isWall = instanceId == "wall"
            s.copy(
                placedItems = s.placedItems.map {
                    if (it.instanceId == instanceId) {
                        it.copy(
                            isPlaced = true,
                            posX = posX,
                            posY = posY,
                            posZ = posZ,
                            rotQx = rotQx,
                            rotQy = rotQy,
                            rotQz = rotQz,
                            rotQw = rotQw,
                        )
                    } else it
                },
                pendingPlacementId = if (isFloor || isWall) s.pendingPlacementId else null,
                pendingFloorPlacement = if (isFloor) false else s.pendingFloorPlacement,
                pendingWallPlacement = if (isWall) false else s.pendingWallPlacement,
                floorPlaced = if (isFloor) true else s.floorPlaced,
                wallPlaced = if (isWall) true else s.wallPlaced,
                selectedInstanceId = instanceId
            )
        }
    }

    fun placeFloor() {
        _state.update { it.copy(floorPlaced = true, pendingFloorPlacement = false) }
    }

    fun placeWall() {
        _state.update { it.copy(wallPlaced = true, pendingWallPlacement = false) }
    }

    fun updateBudget(newBudget: Double) {
        _state.update { it.copy(roomConfig = it.roomConfig.copy(budget = newBudget)) }
    }

    fun setWallPaint(paint: WallPaintMaterial) {
        _state.update { it.copy(wallPaint = paint, pendingWallPlacement = true, showCustomizer = false) }
    }

    fun setFloorMaterial(tile: FloorTileMaterial) {
        _state.update { it.copy(floorMaterial = tile, pendingFloorPlacement = true, showCustomizer = false) }
    }

    fun onFloorPlaced() {
        _state.update { it.copy(pendingFloorPlacement = false, floorPlaced = true) }
    }

    fun onWallPlaced() {
        _state.update { it.copy(pendingWallPlacement = false, wallPlaced = true) }
    }

    fun toggleSummary(show: Boolean) {
        _state.update { it.copy(showSummary = show) }
    }

    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }

    fun fetchAISuggestions() {
        viewModelScope.launch {
            _state.update { it.copy(isAiLoading = true, showAI = true) }
            try {
                val s = _state.value
                val suggestions = aiRepository.getSuggestions(
                    s.roomConfig.type, s.roomConfig.width, s.roomConfig.length, s.roomConfig.height, s.roomConfig.budget
                )
                _state.update { it.copy(aiSuggestions = suggestions, isAiLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isAiLoading = false) }
            }
        }
    }

    fun saveDesign() {
        viewModelScope.launch {
            val s = _state.value
            val existingId = s.roomConfig.designId ?: ""
            val design = SavedDesign(
                id = existingId,
                name = "${s.roomConfig.type} Design",
                roomType = s.roomConfig.type,
                widthM = s.roomConfig.width,
                lengthM = s.roomConfig.length,
                heightM = s.roomConfig.height,
                budget = s.roomConfig.budget,
                budgetUsed = s.totalSpent,
                wallPaintId = s.wallPaint.id,
                wallColorHex = s.wallPaint.hex,
                floorMaterialId = s.floorMaterial.id,
                placedItems = s.placedItems
                    .filter { it.isPlaced }
                    .map {
                    PlacedObjectDto(
                        instanceId = it.instanceId,
                        itemId = it.item.id,
                        isPlaced = it.isPlaced,
                        posX = it.posX,
                        posY = it.posY,
                        posZ = it.posZ,
                        rotQx = it.rotQx,
                        rotQy = it.rotQy,
                        rotQz = it.rotQz,
                        rotQw = it.rotQw,
                        rotationDeg = it.rotationDeg,
                        scale = it.scale,
                        selectedColorHex = it.selectedColorHex
                    )
                }
            )

            _state.update { it.copy(isSaving = true) }
            val saved = designRepository.saveDesignReturning(design)

            // Request the UI to capture a screenshot of the AR view for this saved design.
            _state.update {
                it.copy(
                    showSummary = false,
                    pendingThumbnailDesignId = saved.id,
                    screenshotRequestNonce = System.currentTimeMillis()
                )
            }
        }
    }

    fun onScreenshotCaptured(bitmap: android.graphics.Bitmap?) {
        val designId = _state.value.pendingThumbnailDesignId ?: return
        viewModelScope.launch {
            try {
                if (bitmap != null) {
                    val url = designRepository.uploadDesignThumbnail(designId, bitmap)
                    if (url.isNotEmpty()) {
                        designRepository.updateDesignThumbnailUrl(designId, url)
                    }
                }
            } catch (_: Exception) {
                // Screenshot upload failure should not block save.
            } finally {
                _state.update { it.copy(pendingThumbnailDesignId = null, isSaving = false, saveSuccess = true) }
            }
        }
    }
}
