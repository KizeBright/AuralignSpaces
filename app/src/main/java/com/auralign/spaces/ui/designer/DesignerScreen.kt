package com.auralign.spaces.ui.designer

import android.graphics.Bitmap
import android.graphics.Rect
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import android.widget.FrameLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.auralign.spaces.data.model.FurnitureItem
import com.auralign.spaces.data.model.RoomConfig
import com.auralign.spaces.domain.model.FurnitureCategory
import com.auralign.spaces.ui.theme.AuralignColors
import com.auralign.spaces.ui.theme.PrimaryGradient
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.geometries.UvScale
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.texture.ImageTexture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignerScreen(
    roomConfig: RoomConfig,
    onBack: () -> Unit,
    viewModel: DesignerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    var arSupported by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(context) {
        arSupported = try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (_: Exception) {
            false
        }
    }

    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }
    // Tracks instanceIds whose .glb has been downloaded to device cache
    val downloadedModels = remember { mutableStateMapOf<String, String>() } // instanceId -> local file path
    // Tracks the instantiated AnchorNodes / ModelNodes so we can update or clear them
    val anchorNodes = remember { mutableStateMapOf<String, AnchorNode>() } // instanceId -> anchor node
    val modelNodes = remember { mutableStateMapOf<String, ModelNode>() }   // instanceId -> model node
    // Regular set (no Compose reactivity needed — used only for dedup)
    val downloadingIds = remember { mutableSetOf<String>() }

    // Environment overlay nodes (floor + wall)
    var floorAnchor by remember { mutableStateOf<AnchorNode?>(null) }
    var floorPlane by remember { mutableStateOf<PlaneNode?>(null) }
    var wallAnchor by remember { mutableStateOf<AnchorNode?>(null) }
    var wallPlane by remember { mutableStateOf<PlaneNode?>(null) }

    fun modelUnits(placed: com.auralign.spaces.data.model.PlacedObject): Float {
        val footprint = maxOf(placed.item.widthM, placed.item.depthM, 0.5).toFloat()
        return footprint * placed.scale
    }

    fun applyPlacedModelTransform(
        modelNode: ModelNode,
        placed: com.auralign.spaces.data.model.PlacedObject
    ) {
        modelNode.rotation = Rotation(y = placed.rotationDeg)
        modelNode.scaleToUnitCube(modelUnits(placed))
        modelNode.position = Position(y = -modelNode.size.y)
    }

    fun makeModelSelectable(modelNode: ModelNode, instanceId: String) {
        val selectOnTap: (MotionEvent) -> Boolean = { event ->
            if (event.action == MotionEvent.ACTION_UP) {
                viewModel.selectItem(instanceId)
                true
            } else {
                false
            }
        }
        modelNode.isTouchable = true
        modelNode.onTouch = { event, _ -> selectOnTap(event) }
        modelNode.nodes.forEach { child ->
            child.isTouchable = true
            child.onTouch = { event, _ -> selectOnTap(event) }
        }
    }

    val httpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun captureSurfaceViewBitmap(view: SurfaceView): Bitmap = suspendCancellableCoroutine { cont ->
        val w = view.width
        val h = view.height
        if (w <= 0 || h <= 0) {
            cont.resumeWithException(IllegalStateException("SurfaceView has invalid size"))
            return@suspendCancellableCoroutine
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val handler = Handler(Looper.getMainLooper())
        try {
            PixelCopy.request(
                view,
                Rect(0, 0, w, h),
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) cont.resume(bitmap)
                    else cont.resumeWithException(RuntimeException("PixelCopy failed: $result"))
                },
                handler
            )
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

    LaunchedEffect(roomConfig) {
        viewModel.setRoomConfig(roomConfig)
    }

    // Download .glb files for items that have a modelUrl
    LaunchedEffect(state.placedItems, arSceneView) {
        val sv = arSceneView ?: return@LaunchedEffect

        // Remove entries for deleted items
        val currentIds = state.placedItems.map { it.instanceId }.toSet()
        downloadedModels.keys.filter { it !in currentIds }.toList()
            .forEach { id -> 
                downloadedModels.remove(id)
                anchorNodes[id]?.let { node -> sv.removeChildNode(node) }
                anchorNodes.remove(id)
                modelNodes.remove(id)
            }

        // Start downloads for new items
        state.placedItems
            .filter {
                it.item.modelUrl.isNotEmpty()
                    && it.instanceId !in downloadedModels
                    && it.instanceId !in downloadingIds
            }
            .forEach { placed ->
                downloadingIds.add(placed.instanceId)
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val cacheDir = File(context.cacheDir, "glb_cache").also { it.mkdirs() }
                        val glbFile = File(cacheDir, "${placed.instanceId}.glb")
                        if (!glbFile.exists()) {
                            Log.d("AR3D", "Downloading: ${placed.item.modelUrl}")
                            val req = Request.Builder().url(placed.item.modelUrl).build()
                            httpClient.newCall(req).execute().use { resp ->
                                if (!resp.isSuccessful) error("HTTP ${resp.code} for ${placed.item.modelUrl}")
                                val body = resp.body ?: error("Empty body for ${placed.item.modelUrl}")
                                glbFile.outputStream().use { out ->
                                    body.byteStream().use { inp -> inp.copyTo(out) }
                                }
                            }
                            Log.d("AR3D", "Saved ${glbFile.length()} bytes to ${glbFile.absolutePath}")
                        }
                        withContext(Dispatchers.Main) {
                            downloadedModels[placed.instanceId] = glbFile.absolutePath
                            Log.d("AR3D", "Model ready: ${placed.item.name}")
                        }
                    }.onFailure { e ->
                        Log.e("AR3D", "Download failed for ${placed.item.name}: ${e.message}")
                    }
                    downloadingIds.remove(placed.instanceId)
                }
            }
    }

    // Configure AR session for plane detection
    LaunchedEffect(arSceneView) {
        val sv = arSceneView ?: return@LaunchedEffect
        runCatching {
            sv.configureSession { _, config ->
                config.planeFindingMode = com.google.ar.core.Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.instantPlacementMode = com.google.ar.core.Config.InstantPlacementMode.DISABLED
            }
        }
    }

    // MOST IMPORTANT: when saving, capture screenshot and upload
    LaunchedEffect(state.screenshotRequestNonce, arSceneView) {
        if (state.pendingThumbnailDesignId == null) return@LaunchedEffect
        val sv = arSceneView ?: return@LaunchedEffect
        runCatching {
            val bmp = withContext(Dispatchers.Main) { captureSurfaceViewBitmap(sv) }
            viewModel.onScreenshotCaptured(bmp)
        }.onFailure {
            viewModel.onScreenshotCaptured(null)
        }
    }

    // Apply/update floor material (live in AR)
    val floorTextureCache = remember { mutableStateMapOf<String, Bitmap>() } // url -> bitmap
    LaunchedEffect(arSceneView, floorAnchor, state.floorMaterial) {
        val sv = arSceneView ?: return@LaunchedEffect
        val anchorNode = floorAnchor ?: return@LaunchedEffect
        if (anchorNode.parent == null) sv.addChildNode(anchorNode)

        fun makeTileBitmap(typeId: String): Bitmap {
            val size = 256
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            when (typeId) {
                "tile_oak" -> {
                    canvas.drawColor(AndroidColor.parseColor("#8B5A2B"))
                    paint.color = AndroidColor.parseColor("#6B3E1E")
                    paint.strokeWidth = 6f
                    for (x in 0 until size step 32) {
                        canvas.drawLine(x.toFloat(), 0f, x.toFloat(), size.toFloat(), paint)
                    }
                }
                "tile_stone" -> {
                    canvas.drawColor(AndroidColor.parseColor("#7A7F87"))
                    paint.color = AndroidColor.parseColor("#5A5F66")
                    paint.strokeWidth = 4f
                    for (i in 0..8) {
                        val y = i * (size / 8f)
                        canvas.drawLine(0f, y, size.toFloat(), y, paint)
                        canvas.drawLine(y, 0f, y, size.toFloat(), paint)
                    }
                }
                else -> { // tile_marble
                    canvas.drawColor(AndroidColor.parseColor("#F2F2F2"))
                    paint.color = AndroidColor.parseColor("#D0D0D0")
                    paint.strokeWidth = 5f
                    for (index in 0..12) {
                        val x1 = (Math.random() * size).toFloat()
                        val y1 = (Math.random() * size).toFloat()
                        val x2 = (Math.random() * size).toFloat()
                        val y2 = (Math.random() * size).toFloat()
                        canvas.drawLine(x1, y1, x2, y2, paint)
                    }
                }
            }
            return bmp
        }

        suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val bytes = resp.body?.bytes() ?: return@use null
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }.getOrNull()
        }

        val bitmap = if (state.floorMaterial.textureUrl.isNotBlank()) {
            floorTextureCache[state.floorMaterial.textureUrl]
                ?: downloadBitmap(state.floorMaterial.textureUrl)?.also { floorTextureCache[state.floorMaterial.textureUrl] = it }
                ?: makeTileBitmap(state.floorMaterial.id)
        } else {
            makeTileBitmap(state.floorMaterial.id)
        }

        val tex = ImageTexture.Builder()
            .bitmap(bitmap)
            .build(sv.engine)
        val mat = sv.materialLoader.createTextureInstance(
            tex,
            isOpaque = true,
            metallic = 0.0f,
            roughness = state.floorMaterial.roughness,
            reflectance = state.floorMaterial.reflectance
        )

        val plane = floorPlane ?: PlaneNode(
            engine = sv.engine,
            size = Size(3.0f, 3.0f),
            normal = Direction(y = 1.0f),
            uvScale = UvScale(state.floorMaterial.uvScale),
            materialInstance = mat
        ).also {
            floorPlane = it
            anchorNode.addChildNode(it)
        }
        // Replace material instance (simple way to swap texture)
        runCatching {
            plane.materialInstances = listOf(mat)
        }
    }

    // Apply/update wall paint (live in AR)
    LaunchedEffect(arSceneView, wallAnchor, state.wallPaint) {
        val sv = arSceneView ?: return@LaunchedEffect
        val anchorNode = wallAnchor ?: return@LaunchedEffect
        if (anchorNode.parent == null) sv.addChildNode(anchorNode)

        val paintColor = AndroidColor.parseColor(state.wallPaint.hex)
        val mat = sv.materialLoader.createColorInstance(
            io.github.sceneview.math.colorOf(paintColor),

            metallic = 0.0f,
            roughness = state.wallPaint.roughness,
            reflectance = state.wallPaint.reflectance
        )

        val plane = wallPlane ?: PlaneNode(
            engine = sv.engine,
            size = Size(2.5f, 2.5f),
            normal = Direction(z = 1.0f),
            uvScale = UvScale(1.0f),
            materialInstance = mat
        ).also {
            wallPlane = it
            anchorNode.addChildNode(it)
        }
        runCatching {
            plane.materialInstances = listOf(mat)
        }
    }

    // Rebuild anchors/models from saved poses once models are downloaded and AR session exists
    LaunchedEffect(state.placedItems, downloadedModels, arSceneView) {
        val sv = arSceneView ?: return@LaunchedEffect
        val session = sv.session ?: return@LaunchedEffect

        state.placedItems
            .filter { it.isPlaced && it.item.modelUrl.isNotEmpty() && it.instanceId in downloadedModels }
            .forEach { placed ->
                if (placed.instanceId in anchorNodes) return@forEach

                val filePath = downloadedModels[placed.instanceId] ?: return@forEach
                val glbFile = File(filePath)
                if (!glbFile.exists()) return@forEach

                runCatching {
                    val pose = Pose(
                        floatArrayOf(placed.posX, placed.posY, placed.posZ),
                        floatArrayOf(placed.rotQx, placed.rotQy, placed.rotQz, placed.rotQw)
                    )
                    val anchor = session.createAnchor(pose)
                    val anchorNode = AnchorNode(engine = sv.engine, anchor = anchor)

                    val instance = sv.modelLoader.createModelInstance(glbFile)
                    // Set material properties to keep original color
                    instance.materialInstances.forEach { mat ->
                        runCatching {
                            mat.setParameter("metallicFactor", 0.0f)
                            mat.setParameter("roughnessFactor", 1.0f)
                        }
                    }
                    val modelNode = ModelNode(
                        modelInstance = instance,
                        scaleToUnits = modelUnits(placed),
                        centerOrigin = Position(y = -1.0f)
                    )
                    applyPlacedModelTransform(modelNode, placed)
                    makeModelSelectable(modelNode, placed.instanceId)
                    anchorNode.addChildNode(modelNode)

                    sv.addChildNode(anchorNode)
                    anchorNodes[placed.instanceId] = anchorNode
                    modelNodes[placed.instanceId] = modelNode
                }.onFailure { err ->
                    Log.e("AR3D", "Failed to rebuild anchor for ${placed.instanceId}: ${err.message}")
                }
            }

        // Rebuild floor and wall anchors
        state.placedItems.find { it.instanceId == "floor" && it.isPlaced }?.let { placed ->
            runCatching {
                val pose = Pose(
                    floatArrayOf(placed.posX, placed.posY, placed.posZ),
                    floatArrayOf(placed.rotQx, placed.rotQy, placed.rotQz, placed.rotQw)
                )
                val anchor = session.createAnchor(pose)
                floorAnchor = AnchorNode(engine = sv.engine, anchor = anchor)
                sv.addChildNode(floorAnchor!!)
            }.onFailure { err ->
                Log.e("AR3D", "Failed to rebuild floor anchor: ${err.message}")
            }
        }
        state.placedItems.find { it.instanceId == "wall" && it.isPlaced }?.let { placed ->
            runCatching {
                val pose = Pose(
                    floatArrayOf(placed.posX, placed.posY, placed.posZ),
                    floatArrayOf(placed.rotQx, placed.rotQy, placed.rotQz, placed.rotQw)
                )
                val anchor = session.createAnchor(pose)
                wallAnchor = AnchorNode(engine = sv.engine, anchor = anchor)
                sv.addChildNode(wallAnchor!!)
            }.onFailure { err ->
                Log.e("AR3D", "Failed to rebuild wall anchor: ${err.message}")
            }
        }
    }

    // Keep loaded 3D nodes in sync with transforms from the UI controls
    LaunchedEffect(state.placedItems) {
        state.placedItems.forEach { placed ->
            modelNodes[placed.instanceId]?.let { node ->
                applyPlacedModelTransform(node, placed)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AuralignColors.Obsidian)) {
        // --- AR View Area ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp)
        ) {
            // Live AR Camera Background
            if (arSupported == false) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Text(
                        "AR is not supported on this device.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (!hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                    Text(
                        "Camera permission is required to use the AR designer.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        runCatching {
                            ARSceneView(ctx).also { sv ->
                                arSceneView = sv
                                sv.onTouchEvent = { e, _ ->
                                    if (e.action == MotionEvent.ACTION_UP) {
                                        val s = viewModel.state.value
                                        when {
                                            s.pendingPlacementId != null -> {
                                                val hit = sv.hitTestAR(
                                                    xPx = e.x,
                                                    yPx = e.y,
                                                    planeTypes = setOf(Plane.Type.HORIZONTAL_UPWARD_FACING),
                                                    instantPlacementPoint = false
                                                )
                                                if (hit != null) {
                                                    runCatching {
                                                        val anchor = hit.createAnchor()
                                                        val pose = anchor.pose
                                                        val t = pose.translation
                                                        val r = pose.rotationQuaternion

                                                        if (s.pendingPlacementId == "floor") {
                                                            floorAnchor?.let { sv.removeChildNode(it) }
                                                            floorAnchor = AnchorNode(engine = sv.engine, anchor = anchor)
                                                            sv.addChildNode(floorAnchor!!)
                                                        } else if (s.pendingPlacementId == "wall") {
                                                            wallAnchor?.let { sv.removeChildNode(it) }
                                                            wallAnchor = AnchorNode(engine = sv.engine, anchor = anchor)
                                                            sv.addChildNode(wallAnchor!!)
                                                        }

                                                        viewModel.placeItemFromPose(
                                                            instanceId = s.pendingPlacementId!!,
                                                            posX = t[0],
                                                            posY = t[1],
                                                            posZ = t[2],
                                                            rotQx = r[0],
                                                            rotQy = r[1],
                                                            rotQz = r[2],
                                                            rotQw = r[3],
                                                        )

                                                        if (s.pendingPlacementId !in listOf("floor", "wall")) {
                                                            val movedId = s.pendingPlacementId!!
                                                            anchorNodes[movedId]?.let { oldNode -> sv.removeChildNode(oldNode) }
                                                            anchorNodes.remove(movedId)
                                                            modelNodes.remove(movedId)
                                                            anchor.detach()
                                                        }
                                                    }
                                                }
                                                true
                                            }
                                            s.pendingFloorPlacement -> {
                                                val hit = sv.hitTestAR(
                                                    xPx = e.x,
                                                    yPx = e.y,
                                                    planeTypes = setOf(Plane.Type.HORIZONTAL_UPWARD_FACING),
                                                    instantPlacementPoint = false
                                                )
                                                if (hit != null) {
                                                    runCatching {
                                                        val anchor = hit.createAnchor()
                                                        val pose = anchor.pose
                                                        val t = pose.translation
                                                        val r = pose.rotationQuaternion

                                                        floorAnchor?.let { sv.removeChildNode(it) }
                                                        floorAnchor = AnchorNode(engine = sv.engine, anchor = anchor)
                                                        sv.addChildNode(floorAnchor!!)

                                                        viewModel.placeItemFromPose(
                                                            instanceId = "floor",
                                                            posX = t[0],
                                                            posY = t[1],
                                                            posZ = t[2],
                                                            rotQx = r[0],
                                                            rotQy = r[1],
                                                            rotQz = r[2],
                                                            rotQw = r[3],
                                                        )
                                                    }
                                                }
                                                true
                                            }
                                            s.pendingWallPlacement -> {
                                                val hit = sv.hitTestAR(
                                                    xPx = e.x,
                                                    yPx = e.y,
                                                    planeTypes = setOf(Plane.Type.VERTICAL),
                                                    instantPlacementPoint = false
                                                )
                                                if (hit != null) {
                                                    runCatching {
                                                        val anchor = hit.createAnchor()
                                                        val pose = anchor.pose
                                                        val t = pose.translation
                                                        val r = pose.rotationQuaternion

                                                        wallAnchor?.let { sv.removeChildNode(it) }
                                                        wallAnchor = AnchorNode(engine = sv.engine, anchor = anchor)
                                                        sv.addChildNode(wallAnchor!!)

                                                        viewModel.placeItemFromPose(
                                                            instanceId = "wall",
                                                            posX = t[0],
                                                            posY = t[1],
                                                            posZ = t[2],
                                                            rotQx = r[0],
                                                            rotQy = r[1],
                                                            rotQz = r[2],
                                                            rotQw = r[3],
                                                        )
                                                    }
                                                }
                                                true
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                            }
                        }.getOrElse {
                            FrameLayout(ctx)
                        }
                    }
                )
            }

            // Placement hint
            AnimatedVisibility(
                visible = state.anyPendingPlacement,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                val hintText = when {
                    state.pendingPlacementId != null -> "Tap on the floor to place"
                    state.pendingFloorPlacement -> "Tap on the floor to place floor tiles"
                    state.pendingWallPlacement -> "Tap on the wall to place wall paint"
                    else -> "Tap to place"
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hintText, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Move your phone to detect surfaces", color = AuralignColors.TextSecondary, fontSize = 12.sp)
                }
            }

        }

        // Top Overlay
        DesignerTopBar(
            roomName = roomConfig.type,
            dimensions = "${roomConfig.width}m × ${roomConfig.length}m",
            itemCount = state.placedItems.size,
            onBack = onBack,
            onSave = { viewModel.saveDesign() },
            onOpenSummary = { viewModel.toggleSummary(true) }
        )

        // Budget Display
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 116.dp) // 100dp for top bar + 16dp padding
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Budget Remaining: ₹${state.budgetRemaining.toInt()}",
                color = if (state.isOverBudget) Color.Red else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Customizer Toggles (Wall/Floor)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            CustomizerActionButton(Icons.Default.AutoAwesome, "AI Suggestions", AuralignColors.Violet) { viewModel.fetchAISuggestions() }
        }

        // Bottom Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(AuralignColors.Midnight)
                .padding(bottom = 16.dp)
        ) {
            if (state.showCustomizer) {
                CustomizerShelf(
                    tab = state.customizerTab,
                    viewModel = viewModel
                )
            } else {
                CategoryTabs(selected = state.activeCategory, onSelect = { viewModel.selectCategory(it) })
                BudgetProgressBar(percent = state.budgetPercent, remaining = state.budgetRemaining, isOver = state.isOverBudget)
                FurnitureShelf(items = state.visibleCatalog.filter { it.category == state.activeCategory }, budgetRemaining = state.budgetRemaining, onItemClick = { viewModel.addItem(it) })
            }
        }

        // Selected Item Controls
        AnimatedVisibility(
            visible = state.selectedInstanceId != null && !state.showCustomizer,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 220.dp)
        ) {
            state.selectedItem?.let { selected ->
                ItemControls(
                    item = selected,
                    onMove = { viewModel.moveItemToPlane(selected.instanceId) },
                    onRotate = { viewModel.updateItemTransform(selected.instanceId, rotation = selected.rotationDeg + it) },
                    onScale = { viewModel.updateItemTransform(selected.instanceId, scale = it) },
                    onDelete = { viewModel.removeItem(selected.instanceId) }
                )
            }
        }
        
        if (state.showSummary) {
            SummarySheet(state = state, onDismiss = { viewModel.toggleSummary(false) }, onSave = { viewModel.saveDesign() }, onRemoveItem = { viewModel.removeItem(it) })
        }
        
        if (state.showAI) {
            AISuggestionsSheet(suggestions = state.aiSuggestions, isLoading = state.isAiLoading, onDismiss = { viewModel.toggleAI(false) })
        }
    }
}

@Composable
fun CustomizerActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = AuralignColors.SurfaceCard, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = color,
        contentColor = Color.White,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun CustomizerShelf(tab: Int, viewModel: DesignerViewModel) {
    val state by viewModel.state.collectAsState()
    
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (tab == 0) "Wall Paints" else "Floor Tiles", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.toggleCustomizer(false) }) {
                Icon(Icons.Default.Close, contentDescription = null, tint = AuralignColors.TextSecondary)
            }
        }
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (tab == 0) {
                items(state.wallPaintOptions) { paint ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(74.dp)
                            .clickable { viewModel.setWallPaint(paint) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(AndroidColor.parseColor(paint.hex).toLong() and 0xFFFFFFFF))
                                .border(if (state.wallPaint.id == paint.id) 3.dp else 0.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${paint.price.toInt()}",
                            color = AuralignColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                items(state.floorMaterialOptions) { tile ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(74.dp)
                            .clickable { viewModel.setFloorMaterial(tile) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuralignColors.SurfaceCard)
                                .border(if (state.floorMaterial.id == tile.id) 2.dp else 0.dp, AuralignColors.Electric, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⬜", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${tile.price.toInt()}",
                            color = AuralignColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DesignerTopBar(
    roomName: String,
    dimensions: String,
    itemCount: Int,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onOpenSummary: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Brush.verticalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(0.5f))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(roomName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(dimensions, color = AuralignColors.TextSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            IconButton(onClick = onSave, modifier = Modifier.size(40.dp).clip(CircleShape).background(AuralignColors.Emerald)) {
                Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(modifier = Modifier.clickable { onOpenSummary() }.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(0.5f)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(itemCount.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryTabs(selected: FurnitureCategory, onSelect: (FurnitureCategory) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(FurnitureCategory.entries) { category ->
            val isSelected = category == selected
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryGradient else androidx.compose.ui.graphics.SolidColor(AuralignColors.Surface))
                    .clickable { onSelect(category) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${category.emoji} ${category.label}",
                    color = if (isSelected) Color.White else AuralignColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BudgetProgressBar(percent: Float, remaining: Double, isOver: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budget", color = AuralignColors.TextSecondary, fontSize = 10.sp)
            Text("₹${remaining.toInt()} left", color = if (isOver) AuralignColors.Rose else AuralignColors.Emerald, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percent },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = if (isOver) AuralignColors.Rose else AuralignColors.Emerald,
            trackColor = AuralignColors.Surface
        )
    }
}

@Composable
fun FurnitureShelf(
    items: List<FurnitureItem>,
    budgetRemaining: Double,
    onItemClick: (FurnitureItem) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            val canAfford = budgetRemaining >= item.price
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clickable(enabled = canAfford) { onItemClick(item) }
                    .alpha(if (canAfford) 1f else 0.4f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuralignColors.SurfaceCard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.name, color = AuralignColors.TextPrimary, fontSize = 9.sp, maxLines = 2, fontWeight = FontWeight.Medium)
                Text("₹${(item.price / 1000).toInt()}k", color = AuralignColors.Emerald, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ItemControls(
    item: com.auralign.spaces.data.model.PlacedObject,
    onMove: () -> Unit,
    onRotate: (Float) -> Unit,
    onScale: (Float) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AuralignColors.Midnight.copy(0.9f)),
        border = BorderStroke(1.dp, AuralignColors.Border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.item.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(item.item.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onMove, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.OpenWith, contentDescription = "Move", tint = Color.White)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = AuralignColors.Rose)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rotate", color = AuralignColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                IconButton(onClick = { onRotate(-15f) }) { Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null, tint = Color.White) }
                Text("${item.rotationDeg.toInt()}°", color = Color.White, modifier = Modifier.weight(1f), fontSize = 12.sp)
                IconButton(onClick = { onRotate(15f) }) { Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null, tint = Color.White) }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scale", color = AuralignColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                Slider(
                    value = item.scale,
                    onValueChange = onScale,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = AuralignColors.Electric, activeTrackColor = AuralignColors.Electric)
                )
                Text("${(item.scale * 100).toInt()}%", color = Color.White, fontSize = 12.sp, modifier = Modifier.width(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarySheet(
    state: DesignerState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onRemoveItem: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AuralignColors.Midnight
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Room Summary", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStatCard("Items", state.placedItems.size.toString(), Icons.Default.Inventory, AuralignColors.Electric, Modifier.weight(1f))
                SummaryStatCard("Spent", "₹${(state.totalSpent / 1000).toInt()}k", Icons.Default.Payment, AuralignColors.Rose, Modifier.weight(1f))
                SummaryStatCard("Left", "₹${(state.budgetRemaining / 1000).toInt()}k", Icons.Default.Savings, AuralignColors.Emerald, Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Wall/Floor selections (DB-driven) and their costs
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AuralignColors.SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Wall paint", color = AuralignColors.TextSecondary, fontSize = 12.sp)
                        Text("₹${state.wallPaint.price.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(state.wallPaint.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Floor tile", color = AuralignColors.TextSecondary, fontSize = 12.sp)
                        Text("₹${state.floorMaterial.price.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(state.floorMaterial.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(state.placedItems) { placed ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(AuralignColors.Surface), contentAlignment = Alignment.Center) {
                            Text(placed.item.emoji)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(placed.item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(placed.item.brand, color = AuralignColors.TextSecondary, fontSize = 11.sp)
                        }
                        Text("₹${placed.item.price.toInt()}", color = AuralignColors.Emerald, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onRemoveItem(placed.instanceId) }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = AuralignColors.TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp)).background(PrimaryGradient),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Save Design", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SummaryStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AuralignColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(label, color = AuralignColors.TextSecondary, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISuggestionsSheet(
    suggestions: List<com.auralign.spaces.data.model.AISuggestion>,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AuralignColors.Midnight
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI Design Ideas", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuralignColors.Violet)
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AuralignColors.Violet)
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(suggestions) { suggestion ->
                        AISuggestionCard(suggestion)
                    }
                }
            }
        }
    }
}

@Composable
fun AISuggestionCard(suggestion: com.auralign.spaces.data.model.AISuggestion) {
    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AuralignColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(suggestion.style, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuralignColors.Violet)
            Text(suggestion.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(suggestion.description, fontSize = 12.sp, color = AuralignColors.TextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            suggestion.recommendedItems.forEach { item ->
                Text("• $item", fontSize = 11.sp, color = AuralignColors.TextPrimary, modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(AndroidColor.parseColor(suggestion.wallColor).toLong() and 0xFFFFFFFF)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Est: ₹${suggestion.estimatedCostInr}", color = AuralignColors.Emerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
