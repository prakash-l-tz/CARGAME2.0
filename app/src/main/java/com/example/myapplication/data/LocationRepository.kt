package com.example.myapplication.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

sealed class TrackResult {
    data class Found(val location: PhoneLocation) : TrackResult()
    object NotFound : TrackResult()
    data class Error(val message: String) : TrackResult()
}

class LocationRepository(private val context: Context) {

    // Firebase Realtime Database reference
    private val db = Firebase.database.reference.child("live_locations")

    // Fused Location Provider for real GPS
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // ─── GPS: Get device's current location ──────────────────────────────────

    @SuppressLint("MissingPermission")
    suspend fun getCurrentGPS(): android.location.Location? =
        suspendCancellableCoroutine { cont ->
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            fusedClient.getCurrentLocation(request, null)
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { cont.resume(null) }

            cont.invokeOnCancellation { }
        }

    // ─── Geocoder: lat/lng → address ─────────────────────────────────────────

    suspend fun reverseGeocode(lat: Double, lng: Double): AddressInfo =
        suspendCancellableCoroutine { cont ->
            val geocoder = Geocoder(context, Locale("en", "IN"))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    cont.resume(addresses.firstOrNull()?.toAddressInfo() ?: AddressInfo.unknown(lat, lng))
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = try {
                    geocoder.getFromLocation(lat, lng, 1)
                } catch (e: Exception) {
                    null
                }
                cont.resume(addresses?.firstOrNull()?.toAddressInfo() ?: AddressInfo.unknown(lat, lng))
            }
        }

    // ─── Firebase: Save location under phone number ───────────────────────────

    suspend fun shareLocation(
        phoneNumber: String,
        gpsLocation: android.location.Location,
        batteryLevel: Int
    ): Result<Unit> = suspendCancellableCoroutine { cont ->

        val addressInfo = runCatching {
            @Suppress("DEPRECATION")
            val geo = Geocoder(context, Locale("en", "IN"))
            geo.getFromLocation(gpsLocation.latitude, gpsLocation.longitude, 1)?.firstOrNull()?.toAddressInfo()
                ?: AddressInfo.unknown(gpsLocation.latitude, gpsLocation.longitude)
        }.getOrElse { AddressInfo.unknown(gpsLocation.latitude, gpsLocation.longitude) }

        val data = FirebaseLocation(
            phoneNumber = phoneNumber,
            latitude = gpsLocation.latitude,
            longitude = gpsLocation.longitude,
            area = addressInfo.area,
            city = addressInfo.city,
            state = addressInfo.state,
            pincode = addressInfo.pincode,
            fullAddress = addressInfo.fullAddress,
            timestamp = System.currentTimeMillis(),
            accuracy = gpsLocation.accuracy,
            operator = "Unknown",
            networkType = "GPS",
            batteryLevel = batteryLevel
        )

        db.child(phoneNumber).setValue(data)
            .addOnSuccessListener { cont.resume(Result.success(Unit)) }
            .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
    }

    // ─── Firebase: Track a phone number (real-time listener) ─────────────────

    fun trackPhoneNumber(phoneNumber: String): Flow<TrackResult> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(TrackResult.NotFound)
                    return
                }
                try {
                    val data = snapshot.getValue(FirebaseLocation::class.java)
                    if (data == null) {
                        trySend(TrackResult.NotFound)
                        return
                    }
                    // isLive = updated within last 5 minutes
                    val ageMs = System.currentTimeMillis() - data.timestamp
                    val isLive = ageMs < 5 * 60 * 1000L
                    trySend(TrackResult.Found(data.toPhoneLocation(isLive)))
                } catch (e: Exception) {
                    trySend(TrackResult.Error(e.message ?: "Parse error"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(TrackResult.Error(error.message))
            }
        }

        val ref = db.child(phoneNumber)
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}

// ─── Address helper ──────────────────────────────────────────────────────────

data class AddressInfo(
    val area: String,
    val city: String,
    val state: String,
    val pincode: String,
    val fullAddress: String
) {
    companion object {
        fun unknown(lat: Double, lng: Double) = AddressInfo(
            area = "Unknown Area",
            city = "Unknown City",
            state = "Tamil Nadu",
            pincode = "000000",
            fullAddress = "Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}"
        )
    }
}

private fun android.location.Address.toAddressInfo(): AddressInfo {
    val area = subLocality ?: thoroughfare ?: featureName ?: "Unknown Area"
    val city = locality ?: subAdminArea ?: adminArea ?: "Unknown City"
    val state = adminArea ?: "Unknown State"
    val pin = postalCode ?: "000000"
    val full = (0..maxAddressLineIndex).joinToString(", ") { getAddressLine(it) }
    return AddressInfo(area = area, city = city, state = state, pincode = pin, fullAddress = full)
}
