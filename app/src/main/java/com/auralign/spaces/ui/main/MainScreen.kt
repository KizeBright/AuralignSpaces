package com.auralign.spaces.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.auralign.spaces.data.model.RoomConfig
import com.auralign.spaces.ui.home.HomeScreen
import com.auralign.spaces.ui.profile.ProfileScreen
import com.auralign.spaces.ui.saved.SavedDesignsScreen
import com.auralign.spaces.ui.saved.ThumbnailViewerScreen
import com.google.firebase.auth.FirebaseAuth

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("home",    "Home",    Icons.Rounded.OtherHouses),
    NavItem("saved",   "Saved",   Icons.Rounded.BookmarkBorder),
    NavItem("profile", "Profile", Icons.Rounded.PersonOutline)
)

@Composable
fun MainScreen(
    onNavigateToDesigner: (RoomConfig) -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            NavigationBar(
                containerColor = cs.surface,
                tonalElevation = 4.dp
            ) {
                val entry by navController.currentBackStackEntryAsState()
                val current = entry?.destination
                navItems.forEach { item ->
                    val selected = current?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(item.icon, item.label, modifier = Modifier.size(24.dp))
                        },
                        label = {
                            Text(item.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = cs.primary,
                            selectedTextColor   = cs.primary,
                            unselectedIconColor = cs.onSurfaceVariant,
                            unselectedTextColor = cs.onSurfaceVariant,
                            indicatorColor      = cs.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val user = FirebaseAuth.getInstance().currentUser
                val name = user?.displayName ?: user?.email?.substringBefore("@") ?: "Designer"
                HomeScreen(userName = name, onNavigateToDesigner = onNavigateToDesigner)
            }
            composable("saved") {
                SavedDesignsScreen(
                    onNavigateToHome = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                    onEditDesign = { savedDesign ->
                        onNavigateToDesigner(RoomConfig(
                            type = savedDesign.roomType,
                            width = savedDesign.widthM,
                            length = savedDesign.lengthM,
                            height = savedDesign.heightM,
                            budget = savedDesign.budget,
                            designId = savedDesign.id
                        ))
                    },
                    onViewThumbnail = { savedDesign ->
                        navController.navigate("thumbnail_viewer/${savedDesign.id}")
                    }
                )
            }
            composable("thumbnail_viewer/{designId}") { backStackEntry ->
                val designId = backStackEntry.arguments?.getString("designId") ?: return@composable
                ThumbnailViewerScreen(designId = designId, onBack = { navController.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(onSignOut = onSignOut)
            }
        }
    }
}
