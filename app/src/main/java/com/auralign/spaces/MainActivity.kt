package com.auralign.spaces

import android.os.Bundle
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auralign.spaces.data.local.SettingsManager
import com.auralign.spaces.data.model.RoomConfig
import com.auralign.spaces.ui.auth.AuthScreen
import com.auralign.spaces.ui.designer.DesignerScreen
import com.auralign.spaces.ui.home.HomeScreen
import com.auralign.spaces.ui.splash.SplashScreen
import com.auralign.spaces.ui.theme.AuralignTheme
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by settingsManager.isDarkMode.collectAsState(initial = false)

            AuralignTheme(darkTheme = isDarkMode) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(R.drawable.home_texture_background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = if (isDarkMode) 0.58f else 0.70f))
                    )
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        AuralignNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun AuralignNavigation() {
    val navController = rememberNavController()
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                val nextDest = if (auth.currentUser != null) "main" else "auth"
                navController.navigate(nextDest) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("auth") {
            AuthScreen(onAuthSuccess = {
                navController.navigate("main") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("main") {
            com.auralign.spaces.ui.main.MainScreen(
                onNavigateToDesigner = { config ->
                    val configJson = Gson().toJson(config)
                    val encodedConfigJson = Uri.encode(configJson)
                    Log.d("AuralignNavigation", "Navigate to designer with encoded config: $encodedConfigJson")
                    navController.navigate("designer/$encodedConfigJson")
                },
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = "designer/{configJson}",
            arguments = listOf(navArgument("configJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedConfigJson = backStackEntry.arguments?.getString("configJson")
            val configJson = encodedConfigJson?.let { Uri.decode(it) }
            Log.d("AuralignNavigation", "Designer route configJson: $configJson")
            val config = Gson().fromJson(configJson, RoomConfig::class.java)
            DesignerScreen(
                roomConfig = config,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
