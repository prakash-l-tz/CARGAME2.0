package com.example.myapplication.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class InputError { NONE, TOO_SHORT, INVALID_START }

enum class SearchState { IDLE, SEARCHING, FOUND, NOT_FOUND, ERROR }

enum class ShareState { IDLE, GETTING_GPS, SAVING, SUCCESS, PERMISSION_DENIED, ERROR }

data class LocationUiState(
    val phoneInput: String = "",
    val inputError: InputError = InputError.NONE,
    val isInputValid: Boolean = false,
    val searchState: SearchState = SearchState.IDLE,
    val shareState: ShareState = ShareState.IDLE,
    val foundLocation: PhoneLocation? = null,
    val errorMessage: String = "",
    val recentSearches: List<RecentSearch> = emptyList(),
    val isDarkMode: Boolean = false,
    val sharePhone: String = "",       // number being shared
    val isShareInputValid: Boolean = false
)

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = LocationRepository(application)
    private val prefs = application.getSharedPreferences("loc_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null

    init {
        loadRecentSearches()
    }

    // ─── Phone input (search) ────────────────────────────────────────────────

    fun onPhoneInputChange(input: String) {
        val digits = input.filter { it.isDigit() }.take(10)
        val error = when {
            digits.isEmpty() -> InputError.NONE
            digits.isNotEmpty() && !digits[0].toString().matches(Regex("[6-9]")) -> InputError.INVALID_START
            digits.length < 10 -> InputError.TOO_SHORT
            else -> InputError.NONE
        }
        _uiState.update {
            it.copy(
                phoneInput = digits,
                inputError = error,
                isInputValid = isValidIndianMobile(digits),
                searchState = SearchState.IDLE,
                foundLocation = null
            )
        }
    }

    // ─── Search / Track ──────────────────────────────────────────────────────

    fun searchLocation() {
        val number = _uiState.value.phoneInput
        if (!isValidIndianMobile(number)) return

        trackingJob?.cancel()
        _uiState.update { it.copy(searchState = SearchState.SEARCHING) }

        trackingJob = viewModelScope.launch {
            repo.trackPhoneNumber(number)
                .collect { result ->
                    when (result) {
                        is TrackResult.Found -> {
                            _uiState.update {
                                it.copy(
                                    searchState = SearchState.FOUND,
                                    foundLocation = result.location
                                )
                            }
                            addRecentSearch(number, result.location.city)
                        }
                        is TrackResult.NotFound -> {
                            _uiState.update {
                                it.copy(
                                    searchState = SearchState.NOT_FOUND,
                                    errorMessage = "This number hasn't shared their location yet.\nAsk them to open the app and tap \"Share My Location\"."
                                )
                            }
                        }
                        is TrackResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    searchState = SearchState.ERROR,
                                    errorMessage = result.message
                                )
                            }
                        }
                    }
                }
        }
    }

    fun clearSearch() {
        trackingJob?.cancel()
        _uiState.update {
            it.copy(
                phoneInput = "",
                inputError = InputError.NONE,
                isInputValid = false,
                searchState = SearchState.IDLE,
                foundLocation = null,
                errorMessage = ""
            )
        }
    }

    // ─── Share my location ───────────────────────────────────────────────────

    fun onSharePhoneChange(input: String) {
        val digits = input.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(sharePhone = digits, isShareInputValid = isValidIndianMobile(digits)) }
    }

    fun shareMyLocation(hasPermission: Boolean) {
        if (!hasPermission) {
            _uiState.update { it.copy(shareState = ShareState.PERMISSION_DENIED) }
            return
        }
        val number = _uiState.value.sharePhone
        if (!isValidIndianMobile(number)) return

        viewModelScope.launch {
            _uiState.update { it.copy(shareState = ShareState.GETTING_GPS) }

            val gpsLocation = repo.getCurrentGPS()
            if (gpsLocation == null) {
                _uiState.update {
                    it.copy(
                        shareState = ShareState.ERROR,
                        errorMessage = "Could not get GPS. Enable location & try again."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(shareState = ShareState.SAVING) }
            val battery = getBatteryLevel()

            val result = repo.shareLocation(number, gpsLocation, battery)
            if (result.isSuccess) {
                prefs.edit().putString("my_number", number).apply()
                _uiState.update {
                    it.copy(
                        shareState = ShareState.SUCCESS,
                        sharePhone = number
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        shareState = ShareState.ERROR,
                        errorMessage = result.exceptionOrNull()?.message ?: "Save failed"
                    )
                }
            }
        }
    }

    fun resetShareState() {
        _uiState.update { it.copy(shareState = ShareState.IDLE) }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    fun selectRecent(phoneNumber: String) {
        onPhoneInputChange(phoneNumber)
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun getErrorMessage(error: InputError): String = when (error) {
        InputError.NONE -> ""
        InputError.TOO_SHORT -> "Enter 10-digit number"
        InputError.INVALID_START -> "Must start with 6, 7, 8 or 9"
    }

    fun getSavedMyNumber(): String = prefs.getString("my_number", "") ?: ""

    private fun getBatteryLevel(): Int {
        val intent = getApplication<Application>().registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 50
    }

    private fun addRecentSearch(phone: String, city: String) {
        val current = _uiState.value.recentSearches.toMutableList()
        current.removeAll { it.phoneNumber == phone }
        current.add(0, RecentSearch(phone, city, "Just now"))
        val updated = current.take(5)
        _uiState.update { it.copy(recentSearches = updated) }
        prefs.edit().putString("recents", updated.joinToString("|") { "${it.phoneNumber},${it.city}" }).apply()
    }

    private fun loadRecentSearches() {
        val saved = prefs.getString("recents", "") ?: ""
        if (saved.isBlank()) return
        val recents = saved.split("|").mapNotNull {
            val parts = it.split(",")
            if (parts.size >= 2) RecentSearch(parts[0], parts[1], "Earlier") else null
        }
        _uiState.update { it.copy(recentSearches = recents) }
    }
}
