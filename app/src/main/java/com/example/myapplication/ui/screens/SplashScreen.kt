package com.example.myapplication.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    val iconScale by rememberInfiniteTransition(label = "icon").animateFloat(
        initialValue = 0.90f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "scale"
    )

    var contentAlpha by remember { mutableFloatStateOf(0f) }
    var progressValue by remember { mutableFloatStateOf(0f) }

    val animatedAlpha by animateFloatAsState(targetValue = contentAlpha, animationSpec = tween(700), label = "alpha")
    val animatedProgress by animateFloatAsState(targetValue = progressValue, animationSpec = tween(2200), label = "progress")

    LaunchedEffect(Unit) {
        contentAlpha = 1f
        progressValue = 1f
        delay(2600)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF12122A), Color(0xFF1A1A3A)))),
        contentAlignment = Alignment.Center
    ) {
        // Decorative circles
        Box(modifier = Modifier.size(320.dp).clip(CircleShape).background(Color(0xFFE63946).copy(alpha = 0.06f)).align(Alignment.TopEnd).offset(x = 80.dp, y = (-80).dp))
        Box(modifier = Modifier.size(220.dp).clip(CircleShape).background(Color(0xFF00FF88).copy(alpha = 0.05f)).align(Alignment.BottomStart).offset(x = (-50).dp, y = 50.dp))

        Column(
            modifier = Modifier.fillMaxWidth().alpha(animatedAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Car icon with glow
            Box(
                modifier = Modifier.size(120.dp).scale(iconScale),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFE63946).copy(alpha = 0.15f)))
                Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFFE63946).copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFFFFD700), modifier = Modifier.size(52.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "🏎️",
                fontSize = 30.sp
            )

            Text(
                "Car Drive Simulator",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Text(
                "Real Car Controls · Gears · Horn · Brakes",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                listOf("ENGINE", "GEARS", "STEERING", "HORN").forEach { label ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE63946).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(label, color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.padding(horizontal = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFE63946),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Starting engine...", color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            }
        }

        Text(
            "5 Gears · Reverse · Off-Road · Odometer",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)
        )
    }
}
