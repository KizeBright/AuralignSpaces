package com.auralign.spaces.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text(design?.name ?: "Design", fontWeight = FontWeight.Bold, color = cs.onBackground, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, null, tint = cs.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(cs.background),
            contentAlignment = Alignment.Center
        ) {
            when {
                design == null -> {
                    CircularProgressIndicator()
                }
                design?.thumbnailUrl?.isNotBlank() == true -> {
                    AsyncImage(
                        model = design?.thumbnailUrl,
                        contentDescription = "Design thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                else -> {
                    Text(
                        "No thumbnail available",
                        color = cs.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}