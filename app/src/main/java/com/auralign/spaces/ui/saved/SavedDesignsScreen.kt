package com.auralign.spaces.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material.icons.rounded.OtherHouses
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SquareFoot
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.auralign.spaces.data.model.SavedDesign

private const val GITHUB_THUMBNAIL_BASE_URL =
    "https://raw.githubusercontent.com/mithra-n/auralign-design-thumbnails/main/thumbnails"

internal fun githubThumbnailUrl(design: SavedDesign): String {
    val url = design.thumbnailUrl.ifBlank { "$GITHUB_THUMBNAIL_BASE_URL/${design.id}.jpg" }
    val separator = if (url.contains("?")) "&" else "?"
    return "$url${separator}updated=${design.updatedAt.seconds}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedDesignsScreen(
    onNavigateToHome: () -> Unit,
    onEditDesign: (SavedDesign) -> Unit,
    onViewThumbnail: (SavedDesign) -> Unit,
    viewModel: SavedDesignsViewModel = hiltViewModel()
) {
    val designs by viewModel.savedDesigns.collectAsState()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("My Rooms", fontWeight = FontWeight.Bold, color = cs.onBackground, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToHome,
                containerColor = cs.primary,
                contentColor = cs.onPrimary,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New Room", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        if (designs.isEmpty()) {
            EmptyState(onNavigateToHome, Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(designs, key = { it.id }) { design ->
                    SavedRoomCard(
                        design = design,
                        onView = { onViewThumbnail(design) },
                        onEdit = { onEditDesign(design) },
                        onDelete = { viewModel.deleteDesign(design.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedRoomCard(
    design: SavedDesign,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        onClick = onView,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = githubThumbnailUrl(design),
                    contentDescription = "${design.name} screenshot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = cs.primary
                        )
                    },
                    error = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ImageNotSupported,
                                null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(34.dp)
                            )
                            Text("Screenshot unavailable", color = cs.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.surface.copy(alpha = 0.88f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.Fullscreen, null, tint = cs.primary, modifier = Modifier.size(14.dp))
                    Text("View", color = cs.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cs.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.OtherHouses, null, tint = cs.primary, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(design.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = cs.onSurface)
                        Text(design.roomType, fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = cs.error, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatChip(
                        icon = Icons.Rounded.SquareFoot,
                        label = "${design.widthM}x${design.lengthM}m",
                        cs = cs
                    )
                    StatChip(
                        icon = Icons.Rounded.Payments,
                        label = "Rs ${(design.budgetUsed / 1000).toInt()}k spent",
                        cs = cs
                    )
                    val pct = if (design.budget > 0.0) {
                        (design.budgetUsed / design.budget).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    StatChip(
                        icon = if (pct > 0.9f) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                        label = "${(pct * 100).toInt()}%",
                        cs = cs,
                        tint = if (pct > 0.9f) cs.error else cs.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(icon: ImageVector, label: String, cs: ColorScheme, tint: Color = cs.onSurfaceVariant) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cs.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(onAction: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.OtherHouses, null, tint = cs.outlineVariant, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("No saved rooms yet", color = cs.onSurfaceVariant, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text("Start by designing a new room.", color = cs.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Design a Room", fontWeight = FontWeight.SemiBold)
        }
    }
}
