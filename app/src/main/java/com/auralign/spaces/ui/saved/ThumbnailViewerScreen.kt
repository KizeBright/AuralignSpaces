package com.auralign.spaces.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.auralign.spaces.data.model.SavedDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailViewerScreen(
    designId: String,
    onBack: () -> Unit,
    viewModel: ThumbnailViewerViewModel = hiltViewModel()
) {
    val design by viewModel.design.collectAsState()
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(designId) {
        viewModel.loadDesign(designId)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        design?.name ?: "Screenshot",
                        fontWeight = FontWeight.Bold,
                        color = cs.onBackground,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = cs.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val currentDesign = design
            if (currentDesign == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            SubcomposeAsyncImage(
                                model = githubThumbnailUrl(currentDesign),
                                contentDescription = "Design screenshot",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 2.dp,
                                        color = cs.primary
                                    )
                                },
                                error = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.ImageNotSupported,
                                            null,
                                            tint = cs.onSurfaceVariant,
                                            modifier = Modifier.size(42.dp)
                                        )
                                        Text(
                                            "Screenshot unavailable",
                                            color = cs.onSurfaceVariant,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            )
                        }
                    }

                    DesignInfoSection(design = currentDesign, cs = cs)
                }
            }
        }
    }
}

@Composable
private fun DesignInfoSection(design: SavedDesign, cs: ColorScheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cs.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(design.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = cs.onBackground)
        Text(design.roomType, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = cs.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${design.widthM} x ${design.lengthM} x ${design.heightM} m", color = cs.onSurface, fontSize = 14.sp)
            Text("Budget Rs ${design.budget.toInt()}", color = cs.onSurface, fontSize = 14.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text("Spent Rs ${design.budgetUsed.toInt()}", color = cs.onSurfaceVariant, fontSize = 14.sp)
    }
}
