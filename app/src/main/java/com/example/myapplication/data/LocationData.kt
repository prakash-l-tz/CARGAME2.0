package com.example.myapplication.data

// Firebase-ல் store ஆகும் model (no-arg constructor required)
data class FirebaseLocation(
    val phoneNumber: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val area: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val fullAddress: String = "",
    val timestamp: Long = 0L,
    val accuracy: Float = 0f,
    val operator: String = "",
    val networkType: String = "",
    val batteryLevel: Int = 0
)

// UI-ல் காட்டப்படும் model
data class PhoneLocation(
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double,
    val area: String,
    val city: String,
    val state: String,
    val pincode: String,
    val fullAddress: String,
    val lastUpdated: String,
    val accuracy: String,
    val operator: String,
    val networkType: String,
    val batteryLevel: Int,
    val isLive: Boolean
)

data class RecentSearch(
    val phoneNumber: String,
    val city: String,
    val time: String
)

fun isValidIndianMobile(number: String): Boolean =
    number.matches(Regex("^[6-9]\\d{9}$"))

fun formatMobile(number: String): String =
    if (number.length == 10) "${number.substring(0, 5)} ${number.substring(5)}" else number

fun FirebaseLocation.toPhoneLocation(isLive: Boolean): PhoneLocation {
    val diffSeconds = (System.currentTimeMillis() - timestamp) / 1000
    val updated = when {
        diffSeconds < 30 -> "Just now"
        diffSeconds < 60 -> "$diffSeconds sec ago"
        diffSeconds < 3600 -> "${diffSeconds / 60} min ago"
        else -> "${diffSeconds / 3600} hr ago"
    }
    return PhoneLocation(
        phoneNumber = phoneNumber,
        latitude = latitude,
        longitude = longitude,
        area = area,
        city = city,
        state = state,
        pincode = pincode,
        fullAddress = fullAddress,
        lastUpdated = updated,
        accuracy = if (accuracy < 20) "High (${accuracy.toInt()}m)" else "~${accuracy.toInt()}m",
        operator = operator,
        networkType = networkType,
        batteryLevel = batteryLevel,
        isLive = isLive
    )
}
