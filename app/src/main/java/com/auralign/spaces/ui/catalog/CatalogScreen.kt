package com.auralign.spaces.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.auralign.spaces.data.model.FurnitureItem
import com.auralign.spaces.domain.model.FurnitureCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onItemClick: (FurnitureItem) -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Catalog", fontWeight = FontWeight.Bold, color = cs.onBackground) },
                actions = {
                    IconButton(onClick = { /* Filter */ }) {
                        Icon(Icons.Rounded.FilterList, null, tint = cs.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder   = { Text("Search for pieces…", color = cs.onSurfaceVariant) },
                leadingIcon   = { Icon(Icons.Rounded.Search, null, tint = cs.primary) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape    = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = cs.primary,
                    unfocusedBorderColor = cs.outline,
                    focusedTextColor     = cs.onSurface,
                    unfocusedTextColor   = cs.onSurface
                )
            )

            // Category chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick  = { viewModel.onCategorySelect(null) },
                        label    = { Text("All", fontWeight = FontWeight.SemiBold) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary,
                            selectedLabelColor     = cs.onPrimary,
                            containerColor         = cs.surfaceVariant,
                            labelColor             = cs.onSurfaceVariant
                        )
                    )
                }
                items(FurnitureCategory.entries) { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick  = { viewModel.onCategorySelect(cat) },
                        label    = { Text(cat.label, fontWeight = FontWeight.Medium) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cs.primary,
                            selectedLabelColor     = cs.onPrimary,
                            containerColor         = cs.surfaceVariant,
                            labelColor             = cs.onSurfaceVariant
                        )
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.filteredItems) { item ->
                        CatalogItemCard(item) { onItemClick(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogItemCard(item: FurnitureItem, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val images = mapOf(
        FurnitureCategory.FURNITURE to "https://images.unsplash.com/photo-1592078615290-033ee584e267?q=80&w=300&auto=format&fit=crop",
        FurnitureCategory.DECOR     to "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?q=80&w=300&auto=format&fit=crop",
        FurnitureCategory.LIGHTING  to "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?q=80&w=300&auto=format&fit=crop",
        FurnitureCategory.RUGS      to "https://images.unsplash.com/photo-1575414003591-ece8d0416c7a?q=80&w=300&auto=format&fit=crop",
        FurnitureCategory.PLANTS    to "https://images.unsplash.com/photo-1485955900006-10f4d324d411?q=80&w=300&auto=format&fit=crop"
    )

    Card(
        modifier  = Modifier.fillMaxWidth().aspectRatio(0.72f).clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AsyncImage(
                    model = images[item.category] ?: "https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=300",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.padding(8.dp).align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = cs.surface.copy(0.9f)
                ) {
                    Icon(
                        Icons.Rounded.FavoriteBorder, null,
                        modifier = Modifier.padding(6.dp).size(16.dp),
                        tint = cs.primary
                    )
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(item.brand.uppercase(), color = cs.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Text(item.name, color = cs.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${item.price.toInt()}", color = cs.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.Star, null, tint = cs.tertiary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(item.rating.toString(), fontSize = 11.sp, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}
