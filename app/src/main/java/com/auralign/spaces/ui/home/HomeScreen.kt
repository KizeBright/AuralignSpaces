package com.auralign.spaces.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.auralign.spaces.data.model.AISuggestion
import com.auralign.spaces.domain.model.RoomType
import com.auralign.spaces.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String = "Designer",
    userPhotoUrl: String? = null,
    onNavigateToDesigner: (com.auralign.spaces.data.model.RoomConfig) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme

    // Time-based greeting
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else      -> "Good night"
        }
    }
    val initial = remember(userName) { userName.trim().take(1).uppercase().ifEmpty { "?" } }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setPhotoUrl(it.toString()) }
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { viewModel.setPhotoUrl(it.toString()) }
    }
    val createUri = {
        val file = java.io.File(context.externalCacheDir, "ar_room_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file).also { cameraUri = it }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) cameraLauncher.launch(createUri())
        else android.widget.Toast.makeText(context, "Camera permission required", android.widget.Toast.LENGTH_SHORT).show()
    }
    val arPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) onNavigateToDesigner(viewModel.getRoomConfig())
        else android.widget.Toast.makeText(context, "Camera permission required for AR designer", android.widget.Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting, fontSize = 13.sp, color = cs.onSurfaceVariant)
                        Text(
                            userName.trim().ifEmpty { "Designer" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onBackground
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SplashGradient)
                            .border(2.dp, cs.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!userPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = userPhotoUrl,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                initial,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Hero / Promo banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardGradient),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        Icons.Rounded.ViewInAr,
                        null,
                        tint = Color.White.copy(0.1f),
                        modifier = Modifier.size(160.dp).align(Alignment.CenterEnd)
                    )
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Plan Your Room", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Set dimensions, place furniture\nin augmented reality.", color = Color.White.copy(0.8f), fontSize = 13.sp)
                    }
                }
            }

            // Room type selection
            item {
                SectionHeader("Room Type", modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                RoomTypeRow(state.selectedRoomType) { viewModel.selectRoomType(it) }
            }

            // 3D Room preview (High-Fidelity)
            item {
                SectionHeader("Room Preview", modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                RoomPreviewCard(state.width, state.length, state.height)
            }

            // Dimension controls
            item {
                DimensionsSection(
                    width = state.width,
                    length = state.length,
                    height = state.height,
                    isOpen = state.isCustomizing,
                    onUpdate = { w, l, h -> viewModel.updateDimensions(w, l, h) },
                    onToggle = { viewModel.toggleCustomizing() }
                )
            }

            // Budget
            item {
                SectionHeader("Budget", modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                BudgetCard(state.budget) { viewModel.updateBudget(it) }
            }

            // AI photo analysis
            item {
                SectionHeader("Room Analysis", modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                AIPhotoCard(
                    photoUrl = state.photoUrl,
                    isAnalyzing = state.isAnalyzing,
                    onGallery = { galleryLauncher.launch("image/*") },
                    onCamera = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) cameraLauncher.launch(createUri())
                        else permLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    onClear = { viewModel.setPhotoUrl(null) },
                    onAnalyze = { viewModel.analyzeRoom() }
                )
            }

            // Show loading indicator
            if (state.isAnalyzing) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = cs.primary)
                            Text("Analyzing your room with AI...", color = cs.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Show error if any
            if (state.analysisError != null) {
                item {
                    Text(
                        state.analysisError!!,
                        color = cs.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Show AI suggestion cards
            if (state.aiSuggestions.isNotEmpty()) {
                item {
                    SectionHeader("AI Design Suggestions", modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp))
                }
                items(state.aiSuggestions) { suggestion ->
                    AISuggestionCard(suggestion = suggestion, cs = cs)
                }
            }

            // Launch CTA
            item {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                            == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            onNavigateToDesigner(viewModel.getRoomConfig())
                        } else {
                            arPermLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor   = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.ViewInAr, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Open AR Designer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun RoomTypeRow(selected: RoomType, onSelect: (RoomType) -> Unit) {
    val cs = MaterialTheme.colorScheme
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(RoomType.entries) { type ->
            val isSelected = type == selected
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) cs.primaryContainer else cs.surfaceVariant)
                    .border(
                        if (isSelected) 2.dp else 0.dp,
                        if (isSelected) cs.primary else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelect(type) }
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(type.emoji, fontSize = 28.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    type.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) cs.primary else cs.onSurfaceVariant
                )
                Text("${type.defaultW}×${type.defaultL}m", fontSize = 10.sp, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RoomPreviewCard(w: Double, l: Double, h: Double) {
    val cs = MaterialTheme.colorScheme
    
    // Architectural Palette
    val wallFillColor       = cs.surface.copy(alpha = 0.9f)
    val floorColor          = cs.surfaceVariant.copy(alpha = 0.6f)
    val gridColor           = cs.outline.copy(alpha = 0.2f)
    val dimensionLineColor  = cs.primary
    val onSurfaceVariant    = cs.onSurfaceVariant

    var started by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        targetValue = if (started) 1f else 0.8f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "roomPreviewScale"
    )
    LaunchedEffect(Unit) { started = true }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(horizontal = 16.dp)
            .scale(scaleAnim),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(0.4f)),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(Modifier.fillMaxSize()) {
            // technical background grid
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val step = 15.dp.toPx()
                for (x in 0..(size.width / step).toInt()) {
                    drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height), 0.5f)
                }
                for (y in 0..(size.height / step).toInt()) {
                    drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step), 0.5f)
                }
            }

            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(32.dp)) {
                val cx = size.width / 2f
                val cy = size.height * 0.65f
                
                val maxDim = maxOf(w, l).toFloat().coerceAtLeast(1f)
                val unit   = (size.width * 0.45f) / maxDim
                
                val rw = (w * unit).toFloat()
                val rl = (l * unit).toFloat()
                val rh = (h * unit).toFloat()
                
                // Isometric conversion
                val isoX = 0.866f
                val isoY = 0.5f
                
                val pFBottom = Offset(cx, cy)
                val pLBottom = Offset(cx - rw * isoX, cy - rw * isoY)
                val pRBottom = Offset(cx + rl * isoX, cy - rl * isoY)
                val pTBottom = Offset(cx - rw * isoX + rl * isoX, cy - rw * isoY - rl * isoY)
                
                val floorPath = Path().apply {
                    moveTo(pFBottom.x, pFBottom.y)
                    lineTo(pLBottom.x, pLBottom.y)
                    lineTo(pTBottom.x, pTBottom.y)
                    lineTo(pRBottom.x, pRBottom.y)
                    close()
                }
                drawPath(floorPath, floorColor)
                drawPath(floorPath, gridColor, style = Stroke(1f))

                // Floor Grid lines (1m steps)
                for (i in 1..w.toInt()) {
                    val sx = cx - (i * unit * isoX); val sy = cy - (i * unit * isoY)
                    drawLine(gridColor, Offset(sx, sy), Offset(sx + rl * isoX, sy - rl * isoY), 1f)
                }
                for (i in 1..l.toInt()) {
                    val sx = cx + (i * unit * isoX); val sy = cy - (i * unit * isoY)
                    drawLine(gridColor, Offset(sx, sy), Offset(sx - rw * isoX, sy - rw * isoY), 1f)
                }

                // Walls
                val pLTop = Offset(pLBottom.x, pLBottom.y - rh)
                val pFTop = Offset(pFBottom.x, pFBottom.y - rh)
                val pRTop = Offset(pRBottom.x, pRBottom.y - rh)
                val pTTop = Offset(pTBottom.x, pTBottom.y - rh)

                val leftWall = Path().apply {
                    moveTo(pLBottom.x, pLBottom.y); lineTo(pTBottom.x, pTBottom.y)
                    lineTo(pTTop.x, pTTop.y); lineTo(pLTop.x, pLTop.y); close()
                }
                drawPath(leftWall, wallFillColor.copy(alpha = 0.8f))
                drawPath(leftWall, cs.outline.copy(0.3f), style = Stroke(1.5f))

                val rightWall = Path().apply {
                    moveTo(pRBottom.x, pRBottom.y); lineTo(pTBottom.x, pTBottom.y)
                    lineTo(pTTop.x, pTTop.y); lineTo(pRTop.x, pRTop.y); close()
                }
                drawPath(rightWall, wallFillColor)
                drawPath(rightWall, cs.outline.copy(0.3f), style = Stroke(1.5f))

                // Dimension Lines
                val arrS = 6f
                val wOff = 25f
                val wL = Offset(pLBottom.x - wOff, pLBottom.y + wOff * 0.5f)
                val wR = Offset(pFBottom.x - wOff, pFBottom.y + wOff * 0.5f)
                drawLine(dimensionLineColor, wL, wR, 1.5f)

                val lOff = 25f
                val lL = Offset(pFBottom.x + lOff, pFBottom.y + lOff * 0.5f)
                val lR = Offset(pRBottom.x + lOff, pRBottom.y + lOff * 0.5f)
                drawLine(dimensionLineColor, lL, lR, 1.5f)

                val hOff = 25f
                drawLine(dimensionLineColor, Offset(pRBottom.x + hOff, pRBottom.y), Offset(pRTop.x + hOff, pRTop.y), 1.5f)
            }

            // Dimension labels overlay
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DimBadge("W: ${w}m", cs.primary)
                    DimBadge("L: ${l}m", cs.secondary)
                    DimBadge("H: ${h}m", cs.tertiary)
                }
                Text("3D LAYOUT PERSPECTIVE", modifier = Modifier.align(Alignment.BottomStart), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = onSurfaceVariant.copy(0.4f))
            }
        }
    }
}

@Composable
private fun DimBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DimensionsSection(
    width: Double, length: Double, height: Double,
    isOpen: Boolean, onUpdate: (Double?, Double?, Double?) -> Unit, onToggle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Custom Dimensions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = cs.onBackground, modifier = Modifier.weight(1f))
            Icon(if (isOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = cs.onSurfaceVariant)
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceVariant).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DimRow("Width",  width,  2.0, 20.0) { onUpdate(it, null, null) }
                HorizontalDivider(color = cs.outline.copy(0.3f))
                DimRow("Length", length, 2.0, 20.0) { onUpdate(null, it, null) }
                HorizontalDivider(color = cs.outline.copy(0.3f))
                DimRow("Height", height, 2.0,  5.0) { onUpdate(null, null, it) }
            }
        }
    }
}

@Composable
private fun DimRow(label: String, value: Double, min: Double, max: Double, onChange: (Double) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(70.dp), color = cs.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(cs.surface).padding(horizontal = 4.dp)) {
            IconButton(onClick = { if (value > min) onChange(value - 0.5) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Remove, null, tint = cs.primary, modifier = Modifier.size(16.dp)) }
            Text("${value}m", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = cs.onSurface, modifier = Modifier.padding(horizontal = 12.dp))
            IconButton(onClick = { if (value < max) onChange(value + 0.5) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Rounded.Add, null, tint = cs.primary, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun BudgetCard(budget: Double, onChange: (Double) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val presets = listOf(25000.0 to "₹25k", 50000.0 to "₹50k", 100000.0 to "₹1L", 200000.0 to "₹2L")
    var customText by remember(budget) { mutableStateOf(budget.toInt().toString()) }
    var isEditing by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.tertiary)
                Spacer(Modifier.width(4.dp))
                if (isEditing) {
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { v -> customText = v.filter { it.isDigit() }; v.toDoubleOrNull()?.let { onChange(it) } },
                        modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 26.sp, fontWeight = FontWeight.Black, color = cs.onBackground),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = cs.primary, unfocusedBorderColor = cs.outline), shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Text(budget.toInt().toString(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = cs.onBackground, modifier = Modifier.weight(1f))
                }
                IconButton(onClick = { isEditing = !isEditing }) { Icon(if (isEditing) Icons.Rounded.Check else Icons.Rounded.Edit, null, tint = cs.primary, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (amount, label) ->
                    val selected = budget == amount
                    FilterChip(
                        selected = selected, onClick  = { onChange(amount); customText = amount.toInt().toString(); isEditing = false },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = cs.primary, selectedLabelColor = cs.onPrimary, containerColor = cs.surface, labelColor = cs.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun AIPhotoCard(
    photoUrl: String?,
    isAnalyzing: Boolean,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = cs.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("AI Room Analysis", fontWeight = FontWeight.Bold, color = cs.onBackground, fontSize = 15.sp)
                if (isAnalyzing) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = cs.primary)
                }
            }

            if (photoUrl != null) {
                Spacer(Modifier.height(12.dp))
                // Photo preview with remove button
                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    AsyncImage(
                        photoUrl, null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                            .background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                // Retake / Change row
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retake", fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                // ✨ Prominent Analyze button — user must tap this to use the API
                Button(
                    onClick = onAnalyze,
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyzing...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze with AI", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(
                    "Upload or photograph your room for AI layout suggestions.",
                    color = cs.onSurfaceVariant, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 14.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCamera, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Camera", fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Upload", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AISuggestionCard(suggestion: AISuggestion, cs: androidx.compose.material3.ColorScheme) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(suggestion.style, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.primary)
                Spacer(Modifier.weight(1f))
                val wallColorInt = try { android.graphics.Color.parseColor(suggestion.wallColor) } catch (e: Exception) { android.graphics.Color.WHITE }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(wallColorInt))
                        .border(1.dp, cs.outline, CircleShape)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(suggestion.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = cs.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(suggestion.description, fontSize = 13.sp, color = cs.onSurfaceVariant)
            if (suggestion.recommendedItems.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Recommended Items", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = cs.primary)
                Spacer(Modifier.height(4.dp))
                suggestion.recommendedItems.forEach { item ->
                    Text("• $item", fontSize = 12.sp, color = cs.onSurface, modifier = Modifier.padding(vertical = 1.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(cs.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Est. Cost: ", fontSize = 12.sp, color = cs.onPrimaryContainer)
                Text("₹${suggestion.estimatedCostInr}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
            }
        }
    }
}
