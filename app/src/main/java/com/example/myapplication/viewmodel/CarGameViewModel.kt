package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

data class OpponentCar(
    val id: Int,
    val laneX: Float,   // pixels from road center: -90f, 0f, or 90f
    val screenY: Float, // 0f = top of canvas, 1f = bottom
    val colorId: Int    // 0=blue, 1=yellow, 2=green
)

data class CarGameState(
    val engineOn: Boolean = false,
    val gear: Int = 0,
    val speed: Float = 0f,
    val carX: Float = 0f,
    val carAngle: Float = 0f,
    val roadOffset: Float = 0f,
    val isAccelerating: Boolean = false,
    val isBraking: Boolean = false,
    val isHorning: Boolean = false,
    val isTurningLeft: Boolean = false,
    val isTurningRight: Boolean = false,
    val rpm: Int = 0,
    val odometer: Float = 0f,
    val isOffRoad: Boolean = false,
    // Racing
    val opponents: List<OpponentCar> = emptyList(),
    val score: Int = 0,
    val lives: Int = 3,
    val isGameOver: Boolean = false,
    val invincibleFrames: Int = 0
)

class CarGameViewModel : ViewModel() {

    private val _state = MutableStateFlow(CarGameState())
    val state: StateFlow<CarGameState> = _state.asStateFlow()

    private var loop: Job? = null
    private var nextOppId = 0
    private var frameCount = 0

    init { startLoop() }

    private fun startLoop() {
        loop = viewModelScope.launch {
            while (true) {
                delay(16L) // ~60 fps
                tick()
            }
        }
    }

    private fun tick() {
        val s = _state.value
        frameCount++

        if (s.isGameOver) return

        if (!s.engineOn) {
            _state.value = s.copy(speed = s.speed * 0.88f, rpm = 0)
            return
        }

        if (s.gear == 0) {
            _state.value = s.copy(speed = s.speed * 0.92f, rpm = 850)
            return
        }

        val isRev = s.gear == -1
        val maxSpeed = when (s.gear) {
            -1 -> 3.5f; 1 -> 6f; 2 -> 12f; 3 -> 20f; 4 -> 30f; 5 -> 42f; else -> 0f
        }
        val accel = when (s.gear) {
            -1 -> 0.25f; 1 -> 0.5f; 2 -> 0.8f; 3 -> 1.0f; 4 -> 1.2f; 5 -> 1.5f; else -> 0f
        }

        val offRoadFactor = if (s.isOffRoad) 0.45f else 1f

        var newSpeed = s.speed
        when {
            s.isBraking      -> newSpeed = (newSpeed - 2.2f).coerceAtLeast(0f)
            s.isAccelerating -> newSpeed = (newSpeed + accel * offRoadFactor).coerceAtMost(maxSpeed * offRoadFactor)
            else             -> newSpeed *= 0.96f
        }

        var newAngle = s.carAngle
        if (newSpeed > 0.4f) {
            val turnRate = (newSpeed / maxSpeed) * 3.5f * if (isRev) -1f else 1f
            if (s.isTurningLeft)  newAngle = (newAngle - turnRate).coerceIn(-40f, 40f)
            if (s.isTurningRight) newAngle = (newAngle + turnRate).coerceIn(-40f, 40f)
        }
        if (!s.isTurningLeft && !s.isTurningRight) newAngle *= 0.88f

        val rad = Math.toRadians(newAngle.toDouble())
        val dir = if (isRev) -1f else 1f
        val newRoadOffset = s.roadOffset + newSpeed * dir * cos(rad).toFloat()
        val newCarX = (s.carX + newSpeed * dir * sin(rad).toFloat()).coerceIn(-260f, 260f)
        val offRoad = abs(newCarX) > 135f

        val rpmTarget = if (s.isAccelerating)
            (800 + (newSpeed / maxSpeed) * 6500).toInt()
        else
            (800 + (newSpeed / maxSpeed) * 2200).toInt()

        // --- Opponent cars ---
        val speedRatio = (newSpeed / 42f).coerceIn(0f, 1f)
        // Opponents always move down; faster when player is faster
        val baseOppSpeed = 0.006f + speedRatio * 0.022f

        var newOpps = s.opponents
            .map { o -> o.copy(screenY = o.screenY + baseOppSpeed) }
            .filter { it.screenY < 1.2f }

        // Spawn opponents only when going forward and moving
        val spawnEvery = (65 - speedRatio * 30).toInt().coerceAtLeast(22)
        if (frameCount % spawnEvery == 0 && newOpps.size < 5 && !isRev && newSpeed > 0.5f) {
            val lanes = listOf(-90f, 0f, 90f)
            newOpps = newOpps + OpponentCar(
                id = nextOppId++,
                laneX = lanes.random(),
                screenY = -0.20f,
                colorId = Random.nextInt(3)
            )
        }

        // Collision detection – player car drawn at h*0.60f
        val newInvincible = (s.invincibleFrames - 1).coerceAtLeast(0)
        var newLives = s.lives
        var newGameOver = false
        var finalInvincible = newInvincible

        if (newInvincible == 0) {
            val hit = newOpps.find { opp ->
                opp.screenY in 0.47f..0.73f && abs(opp.laneX - newCarX) < 55f
            }
            if (hit != null) {
                newLives = (s.lives - 1)
                finalInvincible = 100  // ~1.6s grace period
                newOpps = newOpps.filter { it.id != hit.id }
                if (newLives <= 0) newGameOver = true
            }
        }

        // Score: +1 per tick while moving forward
        val newScore = if (!isRev && newSpeed > 1f) s.score + 1 else s.score

        _state.value = s.copy(
            speed            = newSpeed,
            carAngle         = newAngle,
            roadOffset       = newRoadOffset,
            carX             = newCarX,
            rpm              = rpmTarget,
            odometer         = s.odometer + newSpeed * 0.000035f,
            isOffRoad        = offRoad,
            opponents        = newOpps,
            score            = newScore,
            lives            = newLives,
            isGameOver       = newGameOver,
            invincibleFrames = finalInvincible
        )
    }

    fun toggleEngine() {
        val s = _state.value
        if (s.isGameOver) return
        _state.value = if (s.engineOn) s.copy(engineOn = false, gear = 0)
                       else s.copy(engineOn = true)
    }

    fun setGear(g: Int) {
        val s = _state.value
        if (!s.engineOn || s.isGameOver) return
        if (g == -1 && s.speed > 4f) return
        if (g == 0  && s.speed > 6f) return
        _state.value = s.copy(gear = g)
    }

    fun setAccelerating(v: Boolean) { _state.value = _state.value.copy(isAccelerating = v) }
    fun setBraking(v: Boolean)      { _state.value = _state.value.copy(isBraking = v) }
    fun setHorn(v: Boolean)         { _state.value = _state.value.copy(isHorning = v) }
    fun setTurningLeft(v: Boolean)  { _state.value = _state.value.copy(isTurningLeft = v) }
    fun setTurningRight(v: Boolean) { _state.value = _state.value.copy(isTurningRight = v) }

    fun restartGame() {
        nextOppId = 0
        frameCount = 0
        _state.value = CarGameState()
    }

    override fun onCleared() {
        super.onCleared()
        loop?.cancel()
    }
}
