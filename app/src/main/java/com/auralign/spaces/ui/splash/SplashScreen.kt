package com.auralign.spaces.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralign.spaces.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    val splashBackground = if (isDarkTheme) Color(0xFF1B0711) else Color(0xFFFFF6F8)
    val splashImageTint = Color(0xFF6E1A37).copy(alpha = if (isDarkTheme) 0.48f else 0.34f)
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    val titleOffsetX = remember { Animatable(-34f) }
    val taglineOffsetX = remember { Animatable(34f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scale.animateTo(1f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
            launch { alpha.animateTo(1f, animationSpec = tween(700)) }
            launch { titleOffsetX.animateTo(0f, animationSpec = tween(750)) }
            launch { taglineOffsetX.animateTo(0f, animationSpec = tween(durationMillis = 850, delayMillis = 120)) }
        }
        delay(1200)
        alpha.animateTo(0f, animationSpec = tween(350))
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.splash_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(splashImageTint)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
        ) {
            Image(
                painter = painterResource(R.drawable.splash_logo),
                contentDescription = "Auralign Splash Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(96.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Auralign Spaces",
                color = if (isDarkTheme) Color(0xFFFFD6E2) else Color(0xFF6E1A37),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                letterSpacing = 0.sp,
                modifier = Modifier.offset(x = titleOffsetX.value.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Design, Visualize, Place, Perfect",
                color = if (isDarkTheme) Color(0xFFF9AFC5) else Color(0xFFAE2448),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                letterSpacing = 0.6.sp,
                modifier = Modifier.offset(x = taglineOffsetX.value.dp)
            )
        }
    }
}
