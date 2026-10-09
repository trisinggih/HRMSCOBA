package com.jangeby.system.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jangeby.system.data.local.AppSettings
import com.jangeby.system.data.remote.ApiClient
import com.jangeby.system.data.remote.ApiResponse
import com.jangeby.system.data.remote.CompanyLogoData
import com.jangeby.system.ui.theme.Primary
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    appSettings: AppSettings = koinInject()
) {
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textTranslationY = remember { Animatable(20f) }

    var companyLogoUrl by remember { mutableStateOf(appSettings.companyLogoUrl) }
    var companyName by remember { mutableStateOf(appSettings.companyName ?: "JANGEBY") }

    LaunchedEffect(Unit) {
        // Fetch company logo and branding in background, and update cache
        launch {
            try {
                val client = ApiClient.create()
                val response = client.get(ApiClient.BASE_URL + "company/logo")
                if (response.status.value in 200..299) {
                    val apiResponse: ApiResponse<CompanyLogoData> = response.body()
                    if (apiResponse.success && apiResponse.data != null) {
                        apiResponse.data.logo?.let { logo ->
                            val finalUrl = if (logo.startsWith("http://")) logo.replaceFirst("http://", "https://") else logo
                            companyLogoUrl = finalUrl
                            appSettings.companyLogoUrl = finalUrl
                        }
                        if (!apiResponse.data.name.isNullOrBlank()) {
                            val name = apiResponse.data.name
                            companyName = name
                            appSettings.companyName = name
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore fallback to local branding / cached branding
            }
        }

        // Animate Logo: Scale up with Overshoot bounce feel & Alpha fade in (1200ms)
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f, // Overshoot feel
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // Animate Tagline: startDelay = 600ms, duration = 1000ms
        launch {
            delay(600)
            launch {
                textAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1000)
                )
            }
            launch {
                textTranslationY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                )
            }
        }

        // Wait 2500ms then move to Login or Dashboard
        delay(2500)
        val token = try { appSettings.token } catch (e: Exception) { null }
        if (token.isNullOrEmpty()) {
            onNavigateToLogin()
        } else {
            onNavigateToDashboard()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary)
    ) {
        // Center Logo Section (around 0.4 vertical bias)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .graphicsLayer {
                    scaleX = logoScale.value
                    scaleY = logoScale.value
                    alpha = logoAlpha.value
                }
        ) {
            if (!companyLogoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = companyLogoUrl,
                    contentDescription = "Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(85.dp)
                        .fillMaxWidth(0.75f)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = companyName.take(1).ifEmpty { "J" },
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = companyName,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "HR MANAGEMENT SYSTEM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.85f),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Bottom Tagline Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
                .graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textTranslationY.value
                }
        ) {
            Text(
                text = "Premium Employee Experience",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE0E0E0),
                letterSpacing = 0.5.sp
            )
        }
    }
}
