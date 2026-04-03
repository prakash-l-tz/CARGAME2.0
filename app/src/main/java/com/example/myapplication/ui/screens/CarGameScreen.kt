package com.example.myapplication.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.CarGameState
import com.example.myapplication.viewmodel.CarGameViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/* ═══════════════════════════════════════════════════════════════
   CAR GAME SCREEN  –  Racing Edition
   ═══════════════════════════════════════════════════════════════ */

@Composable
fun CarGameScreen(onBack: () -> Unit) {
    val viewModel: CarGameViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    DisposableEffect(Unit) { onDispose { toneGen.release() } }

    LaunchedEffect(state.isHorning) {
        if (state.isHorning) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 10_000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 150, 100, 150), 0)
            }
        } else {
            toneGen.stopTone()
            vibrator.cancel()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A1A))) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameTopBar(state = state, onGameRestart = { viewModel.restartGame() })
            GameCanvas(state = state, modifier = Modifier.weight(1f).fillMaxWidth())
            GameDashboard(state = state, viewModel = viewModel)
        }

        // Game Over overlay
        if (state.isGameOver) {
            GameOverOverlay(score = state.score, onRestart = { viewModel.restartGame() })
        }
    }
}

/* ─── Top Bar ─────────────────────────────────────────────── */

@Composable
private fun GameTopBar(state: CarGameState, onGameRestart: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF12122A))
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Exit
        TextButton(onClick = { activity?.finish() }) {
            Text("EXIT", color = Color(0xFF666688), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Title + score
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                Text("Car Racing", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Text("Score: ${state.score}", color = Color(0xFF00FF88), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Lives
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                Text(
                    if (i < state.lives) "❤️" else "🖤",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
            }
        }
    }
}

/* ─── Canvas (game view) ──────────────────────────────────── */

@Composable
private fun GameCanvas(state: CarGameState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val roadW = w * 0.54f
        val roadL = (w - roadW) / 2f
        val roadR = roadL + roadW

        /* Grass */
        drawRect(Color(0xFF2D5A1B), size = Size(w, h))
        val grassLines = 12
        for (i in 0..grassLines) {
            val lx = i * (w / grassLines)
            drawLine(Color(0xFF265218), Offset(lx, 0f), Offset(lx, h), strokeWidth = 1f)
        }

        /* Road */
        drawRect(Color(0xFF4A4A4A), topLeft = Offset(roadL, 0f), size = Size(roadW, h))
        drawRect(Color(0xFF3E3E3E), topLeft = Offset(roadL + roadW * 0.1f, 0f), size = Size(roadW * 0.8f, h))

        /* Road edge lines */
        drawRect(Color(0xFFFFE000), topLeft = Offset(roadL, 0f), size = Size(5f, h))
        drawRect(Color(0xFFFFE000), topLeft = Offset(roadR - 5f, 0f), size = Size(5f, h))

        /* ── FORWARD: all elements scroll TOP → BOTTOM ── */

        /* Rumble strips */
        val stripH = 30f; val stripGap = 20f
        val stripPeriod = stripH + stripGap
        val stripeOff = state.roadOffset % stripPeriod
        var sy = stripeOff - stripPeriod
        while (sy < h) {
            if (sy + stripH > 0) {
                val top = maxOf(sy, 0f); val bot = minOf(sy + stripH, h)
                drawRect(Color(0x55FF4444), topLeft = Offset(roadL, top), size = Size(8f, bot - top))
                drawRect(Color(0x55FF4444), topLeft = Offset(roadR - 8f, top), size = Size(8f, bot - top))
            }
            sy += stripPeriod
        }

        /* Center dashes */
        val dashH = 55f; val gapH = 35f
        val dashPeriod = dashH + gapH
        val dashOff = state.roadOffset % dashPeriod
        var dy = dashOff - dashPeriod
        while (dy < h) {
            if (dy + dashH > 0) {
                val t = maxOf(dy, 0f); val b = minOf(dy + dashH, h)
                drawRect(Color(0xCCFFFFFF), topLeft = Offset(w / 2f - 3f, t), size = Size(6f, b - t))
            }
            dy += dashPeriod
        }

        /* Trees */
        val treeSpacing = 170f
        val treeOff = state.roadOffset % treeSpacing
        for (i in 0..9) {
            val ty = treeOff + (i - 1) * treeSpacing
            if (ty > -50f && ty < h + 50f) {
                drawTree(roadL - 55f, ty)
                drawTree(roadR + 55f, ty)
            }
        }

        /* Off-road overlay */
        if (state.isOffRoad) drawRect(Color(0x55FF7700), size = Size(w, h))

        /* Speed lines */
        val speedRatio = (state.speed / 42f).coerceIn(0f, 1f)
        if (speedRatio > 0.5f) {
            val alpha = (speedRatio - 0.5f) * 2f * 0.35f
            for (i in 0..10) {
                val lx = roadL + i * (roadW / 10)
                drawLine(Color(1f, 1f, 1f, alpha), Offset(lx, 0f), Offset(lx, h), strokeWidth = 1.5f)
            }
        }

        /* ── Opponent cars (come from top) ── */
        state.opponents.forEach { opp ->
            val ox = w / 2f + opp.laneX
            val oy = h * opp.screenY
            if (oy > -100f && oy < h + 100f) {
                drawOpponentCar(ox, oy, opp.colorId)
            }
        }

        /* Player car */
        val carScrX = w / 2f + state.carX
        val carScrY = h * 0.60f
        // Flash when invincible
        val showCar = state.invincibleFrames == 0 || (state.invincibleFrames % 10 < 7)
        if (showCar) {
            withTransform({ rotate(state.carAngle, pivot = Offset(carScrX, carScrY)) }) {
                drawCar(carScrX, carScrY, state.engineOn, state.speed > 0.5f, state.isBraking)
            }
        }
    }
}

/* ─── Draw: tree ──────────────────────────────────────────── */

private fun DrawScope.drawTree(cx: Float, cy: Float) {
    drawRect(Color(0xFF6B3A2A), topLeft = Offset(cx - 5f, cy + 12f), size = Size(10f, 18f))
    drawCircle(Color(0xFF1A7A0A), radius = 26f, center = Offset(cx, cy))
    drawCircle(Color(0xFF127A04), radius = 18f, center = Offset(cx - 2f, cy - 10f))
    drawCircle(Color(0xFF0F6A02), radius = 12f, center = Offset(cx + 1f, cy - 20f))
}

/* ─── Draw: player car (red, faces UP = forward) ─────────── */

private fun DrawScope.drawCar(cx: Float, cy: Float, engineOn: Boolean, moving: Boolean, braking: Boolean) {
    val bW = 38f; val bH = 72f

    drawRoundRect(Color(0x55000000), topLeft = Offset(cx - bW/2f + 5f, cy - bH/2f + 5f), size = Size(bW, bH), cornerRadius = CornerRadius(10f))

    val wW = 11f; val wH = 20f; val wc = Color(0xFF111111)
    drawRoundRect(wc, topLeft = Offset(cx - bW/2f - wW + 2f, cy - bH/2f + 8f),  size = Size(wW, wH), cornerRadius = CornerRadius(3f))
    drawRoundRect(wc, topLeft = Offset(cx + bW/2f - 2f,      cy - bH/2f + 8f),  size = Size(wW, wH), cornerRadius = CornerRadius(3f))
    drawRoundRect(wc, topLeft = Offset(cx - bW/2f - wW + 2f, cy + bH/2f - 28f), size = Size(wW, wH), cornerRadius = CornerRadius(3f))
    drawRoundRect(wc, topLeft = Offset(cx + bW/2f - 2f,      cy + bH/2f - 28f), size = Size(wW, wH), cornerRadius = CornerRadius(3f))

    val rc = Color(0xFF999999)
    val rcx1 = cx - bW/2f - wW/2f + 2f; val rcx2 = cx + bW/2f + wW/2f - 2f
    drawCircle(rc, 4f, Offset(rcx1, cy - bH/2f + 18f)); drawCircle(rc, 4f, Offset(rcx2, cy - bH/2f + 18f))
    drawCircle(rc, 4f, Offset(rcx1, cy + bH/2f - 18f)); drawCircle(rc, 4f, Offset(rcx2, cy + bH/2f - 18f))

    drawRoundRect(Color(0xFFE63946), topLeft = Offset(cx - bW/2f, cy - bH/2f), size = Size(bW, bH), cornerRadius = CornerRadius(10f))
    drawRoundRect(Color(0xFFC11820), topLeft = Offset(cx - bW/2f + 4f, cy - bH/2f + 16f), size = Size(bW - 8f, bH - 32f), cornerRadius = CornerRadius(7f))

    // Front windshield (top = forward)
    drawRoundRect(Color(0xAAC8E0F8), topLeft = Offset(cx - bW/2f + 5f, cy - bH/2f + 7f), size = Size(bW - 10f, 16f), cornerRadius = CornerRadius(4f))
    // Rear window
    drawRoundRect(Color(0xAAC8E0F8), topLeft = Offset(cx - bW/2f + 5f, cy + bH/2f - 23f), size = Size(bW - 10f, 14f), cornerRadius = CornerRadius(4f))

    // Headlights (front = top of sprite)
    val hlColor = if (engineOn) Color(0xFFFFFF99) else Color(0xFF777777)
    drawCircle(hlColor, 6f, Offset(cx - bW/2f + 7f, cy - bH/2f + 5f))
    drawCircle(hlColor, 6f, Offset(cx + bW/2f - 7f, cy - bH/2f + 5f))
    if (engineOn) {
        drawCircle(Color(0x55FFFF88), 10f, Offset(cx - bW/2f + 7f, cy - bH/2f + 2f))
        drawCircle(Color(0x55FFFF88), 10f, Offset(cx + bW/2f - 7f, cy - bH/2f + 2f))
    }

    // Taillights (rear = bottom of sprite)
    val tlColor = if (braking) Color(0xFFFF2222) else if (moving) Color(0xFFDD0000) else Color(0xFF660000)
    drawCircle(tlColor, 6f, Offset(cx - bW/2f + 7f, cy + bH/2f - 5f))
    drawCircle(tlColor, 6f, Offset(cx + bW/2f - 7f, cy + bH/2f - 5f))
    if (braking) {
        drawCircle(Color(0x66FF2222), 10f, Offset(cx - bW/2f + 7f, cy + bH/2f - 2f))
        drawCircle(Color(0x66FF2222), 10f, Offset(cx + bW/2f - 7f, cy + bH/2f - 2f))
    }
}

/* ─── Draw: opponent car (faces DOWN = toward player) ─────── */

private fun DrawScope.drawOpponentCar(cx: Float, cy: Float, colorId: Int) {
    val bW = 38f; val bH = 72f

    val bodyColor = when (colorId % 3) {
        0    -> Color(0xFF1E90FF)  // blue
        1    -> Color(0xFFFFAA00)  // orange/yellow
        else -> Color(0xFF00CC44)  // green
    }
    val darkColor = when (colorId % 3) {
        0    -> Color(0xFF1460BB)
        1    -> Color(0xFFBB7700)
        else -> Color(0xFF009933)
    }

    drawRoundRect(Color(0x55000000), topLeft = Offset(cx - bW/2f + 5f, cy - bH/2f + 5f), size = Size(bW, bH), cornerRadius = CornerRadius(10f))

    val wW = 11f; val wH = 20f; val wc = Color(0xFF111111)
    drawRoundRect(wc, topLeft = Offset(cx - bW/2f - wW + 2f, cy - bH/2f + 8f),  size = Size(wW, wH), cornerRadius = CornerRadius(3f))
    drawRoundRect(wc, topLeft = Offset(cx + bW/2f - 2f,      cy - bH/2f + 8f),  size = Size(wW, wH), cornerRadius = CornerRadius(3f))
    drawRoundRect(wc, topLeft = Offset(cx - bW/2f - wW + 2f, cy + bH/2f - 28f), size = Size(wW, wH), cornerRadius = CornerRadius(3f))
    drawRoundRect(wc, topLeft = Offset(cx + bW/2f - 2f,      cy + bH/2f - 28f), size = Size(wW, wH), cornerRadius = CornerRadius(3f))

    drawRoundRect(bodyColor, topLeft = Offset(cx - bW/2f, cy - bH/2f), size = Size(bW, bH), cornerRadius = CornerRadius(10f))
    drawRoundRect(darkColor, topLeft = Offset(cx - bW/2f + 4f, cy - bH/2f + 16f), size = Size(bW - 8f, bH - 32f), cornerRadius = CornerRadius(7f))

    // Rear window (top = rear, facing away)
    drawRoundRect(Color(0xAAC8E0F8), topLeft = Offset(cx - bW/2f + 5f, cy - bH/2f + 7f), size = Size(bW - 10f, 16f), cornerRadius = CornerRadius(4f))
    // Front windshield (bottom = front, facing player)
    drawRoundRect(Color(0xAAC8E0F8), topLeft = Offset(cx - bW/2f + 5f, cy + bH/2f - 23f), size = Size(bW - 10f, 14f), cornerRadius = CornerRadius(4f))

    // Headlights at bottom (front faces player = toward bottom of screen)
    drawCircle(Color(0xFFFFFF99), 6f, Offset(cx - bW/2f + 7f, cy + bH/2f - 5f))
    drawCircle(Color(0xFFFFFF99), 6f, Offset(cx + bW/2f - 7f, cy + bH/2f - 5f))
    drawCircle(Color(0x55FFFF88), 10f, Offset(cx - bW/2f + 7f, cy + bH/2f - 2f))
    drawCircle(Color(0x55FFFF88), 10f, Offset(cx + bW/2f - 7f, cy + bH/2f - 2f))

    // Taillights at top (rear = top)
    drawCircle(Color(0xFFDD0000), 6f, Offset(cx - bW/2f + 7f, cy - bH/2f + 5f))
    drawCircle(Color(0xFFDD0000), 6f, Offset(cx + bW/2f - 7f, cy - bH/2f + 5f))
}

/* ─── Game Over overlay ───────────────────────────────────── */

@Composable
private fun GameOverOverlay(score: Int, onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF12122A))
                .padding(horizontal = 40.dp, vertical = 32.dp)
        ) {
            Text("💥", fontSize = 48.sp)
            Text("GAME OVER", color = Color(0xFFFF4444), fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            Text("Score: $score", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onRestart,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AA44))
            ) {
                Text("▶  PLAY AGAIN", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }
}

/* ─── Dashboard ───────────────────────────────────────────── */

@Composable
private fun GameDashboard(state: CarGameState, viewModel: CarGameViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF12122A))
            .navigationBarsPadding()
            .padding(bottom = 4.dp)
    ) {
        // Speed / Gear / RPM
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GaugeBox("km/h", "${(state.speed * 4).toInt()}", if (state.speed > 30f) Color(0xFFFF4444) else Color(0xFF00FF88))
            GearIndicator(state.gear, state.engineOn)
            GaugeBox("RPM", "${state.rpm}", if (state.rpm > 5500) Color(0xFFFF4444) else Color(0xFFFFD700))
        }

        // Gear selector
        GearRow(state, viewModel)

        HorizontalDivider(color = Color(0xFF22224A), thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))

        // Steering + Gas
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoldButton("◄\nLEFT",  state.isTurningLeft,  Color(0xFF1E2A7A), Color(0xFF3D55D4), Modifier.size(70.dp),
                { viewModel.setTurningLeft(true) }, { viewModel.setTurningLeft(false) })
            HoldButton("⛽\nGAS",  state.isAccelerating,  Color(0xFF0A5C2A), Color(0xFF00CC55), Modifier.size(width = 140.dp, height = 70.dp),
                { viewModel.setAccelerating(true) }, { viewModel.setAccelerating(false) })
            HoldButton("RIGHT\n►", state.isTurningRight, Color(0xFF1E2A7A), Color(0xFF3D55D4), Modifier.size(70.dp),
                { viewModel.setTurningRight(true) }, { viewModel.setTurningRight(false) })
        }

        // Engine + Horn + Brake
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.toggleEngine() },
                modifier = Modifier.size(width = 105.dp, height = 58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (state.engineOn) Color(0xFF00AA44) else Color(0xFF880000))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (state.engineOn) "●" else "○", color = Color.White, fontSize = 16.sp)
                    Text(if (state.engineOn) "ENGINE\nON" else "ENGINE\nOFF", color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 11.sp)
                }
            }

            HoldButton("📯\nHORN", state.isHorning, Color(0xFF5C4A00), Color(0xFFFFAA00), Modifier.size(width = 100.dp, height = 58.dp),
                { viewModel.setHorn(true) }, { viewModel.setHorn(false) })

            HoldButton("🛑\nBRAKE", state.isBraking, Color(0xFF660000), Color(0xFFCC0000), Modifier.size(width = 105.dp, height = 58.dp),
                { viewModel.setBraking(true) }, { viewModel.setBraking(false) })
        }

        // Status bar
        when {
            state.isOffRoad  -> Text("⚠️  OFF ROAD – SLOW DOWN!", color = Color(0xFFFF8C00), fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontSize = 12.sp)
            !state.engineOn  -> Text("Press ENGINE ON to start", color = Color(0xFF666688),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontSize = 11.sp)
            state.gear == 0  -> Text("Select a gear to race!", color = Color(0xFF999900),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontSize = 11.sp)
            state.invincibleFrames > 0 -> Text("⚡ NEAR MISS! Be careful!", color = Color(0xFFFF4444),
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp), textAlign = TextAlign.Center, fontSize = 11.sp)
        }
    }
}

/* ─── Gauge / Gear display ────────────────────────────────── */

@Composable
private fun GaugeBox(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF666688), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun GearIndicator(gear: Int, engineOn: Boolean) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A0A1A)).padding(horizontal = 18.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GEAR", color = Color(0xFF666688), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                when (gear) { -1 -> "R"; 0 -> "N"; else -> gear.toString() },
                color = when {
                    !engineOn  -> Color(0xFF444466)
                    gear == -1 -> Color(0xFFFF8800)
                    gear == 0  -> Color(0xFFFFD700)
                    else       -> Color(0xFF00FFAA)
                },
                fontSize = 32.sp, fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun GearRow(state: CarGameState, viewModel: CarGameViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(-1 to "R", 0 to "N", 1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5").forEach { (g, lbl) ->
            val sel = state.gear == g
            Button(
                onClick = { viewModel.setGear(g) },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp),
                enabled = state.engineOn && !state.isGameOver,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        sel && g == -1 -> Color(0xFFFF6600)
                        sel && g == 0  -> Color(0xFFBBAA00)
                        sel            -> Color(0xFF1155EE)
                        else           -> Color(0xFF22224A)
                    },
                    disabledContainerColor = Color(0xFF16162A)
                )
            ) {
                Text(lbl, color = if (sel) Color.White else Color(0xFF6666AA), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
        }
    }
}

/* ─── Press-and-hold button ───────────────────────────────── */

@Composable
private fun HoldButton(
    label: String,
    pressed: Boolean,
    baseColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) activeColor else baseColor)
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            awaitFirstDown(requireUnconsumed = false)
                            launch { onPress() }
                            waitForUpOrCancellation()
                            launch { onRelease() }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}
