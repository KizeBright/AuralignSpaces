package com.auralign.spaces.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.auralign.spaces.data.model.SavedDesign

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
        containerColor =cs.background,
        topBar = {
            TopAppBar(
                title = { Text("My Rooms", fontWeight = FontWeight.Bold, color = cs.onBackground, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToHome,
                containerColor = cs.primary,
                contentColor   = cs.onPrimary,
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(designs, key = { it.id }) { design ->
                    SavedRoomCard(design, onView = { onViewThumbnail(design) }, onEdit = { onEditDesign(design) }, onDelete = { viewModel.deleteDesign(design.id) })
                }
            }
        }
    }
}


@Composable
private fun SavedRoomCard(design: SavedDesign, onView: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            if (design.thumbnailUrl.isNotBlank()) {
                Box(modifier = Modifier.clickable { onView() }) {
                    AsyncImage(
                        model = design.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
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

            // Stats row
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatChip(
                    icon  = Icons.Rounded.SquareFoot,
                    label = "${design.widthM}×${design.lengthM}m",
                    cs    = cs
                )
                StatChip(
                    icon  = Icons.Rounded.Payments,
                    label = "₹${(design.budgetUsed / 1000).toInt()}k spent",
                    cs    = cs
                )
                val pct = (design.budgetUsed / design.budget).toFloat().coerceIn(0f, 1f)
                StatChip(
                    icon  = if (pct > 0.9f) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                    label = "${(pct * 100).toInt()}%",
                    cs    = cs,
                    tint  = if (pct > 0.9f) cs.error else MaterialTheme.colorScheme.tertiary
                )
            }
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, cs: ColorScheme, tint: Color = cs.onSurfaceVariant) {
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
            onClick  = onAction,
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Design a Room", fontWeight = FontWeight.SemiBold)
        }
    }
}
