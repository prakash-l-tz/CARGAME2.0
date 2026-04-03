package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.myapplication.data.PhoneLocation
import com.example.myapplication.ui.components.PulsingDot
import com.example.myapplication.ui.theme.*
import com.example.myapplication.viewmodel.LocationViewModel

@Composable
fun LocationScreen(
    viewModel: LocationViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val location = uiState.foundLocation ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LocationTopBar(location = location, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Real Google Map
            RealMapView(location = location)

            // Phone number banner
            PhoneNumberBanner(number = location.phoneNumber)

            Spacer(modifier = Modifier.height(12.dp))

            // Location detail card
            LocationDetailCard(location = location)

            Spacer(modifier = Modifier.height(12.dp))

            // Network & Device info
            DeviceInfoCard(location = location)

            Spacer(modifier = Modifier.height(12.dp))

            // Coordinates card
            CoordinatesCard(location = location)

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            ActionButtons(onBack = onBack)
        }
    }
}

@Composable
private fun LocationTopBar(location: PhoneLocation, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BlueDark, BluePrimary)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Live Location",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PulsingDot(if (location.isLive) GreenOnline else MediumGray, 7.dp)
                    Text(
                        if (location.isLive) "Live · Real-time tracking" else "Last known location",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (location.isLive) GreenOnline else MediumGray)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    if (location.isLive) "● LIVE" else "OFFLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun RealMapView(location: PhoneLocation) {
    val phoneLatLng = LatLng(location.latitude, location.longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(phoneLatLng, 15f)
    }

    // Animate camera to location when loaded
    LaunchedEffect(phoneLatLng) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(phoneLatLng, 15f)
            ),
            durationMs = 1200
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false,
                mapToolbarEnabled = false,
                compassEnabled = true
            )
        ) {
            // Accuracy circle
            Circle(
                center = phoneLatLng,
                radius = 80.0,
                fillColor = BluePrimary.copy(alpha = 0.10f),
                strokeColor = BluePrimary.copy(alpha = 0.40f),
                strokeWidth = 2f
            )

            // Phone location marker
            Marker(
                state = MarkerState(position = phoneLatLng),
                title = "+91 ${
                    if (location.phoneNumber.length == 10)
                        "${location.phoneNumber.substring(0, 5)} ${location.phoneNumber.substring(5)}"
                    else location.phoneNumber
                }",
                snippet = "${location.area}, ${location.city}, ${location.state}"
            )
        }

        // LIVE badge overlay (top-left of map)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.95f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PulsingDot(if (location.isLive) GreenOnline else MediumGray, 7.dp)
            Text(
                if (location.isLive) "LIVE" else "CACHED",
                style = MaterialTheme.typography.labelSmall,
                color = if (location.isLive) GreenOnline else MediumGray,
                fontWeight = FontWeight.Bold
            )
        }

        // Location name overlay (bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    location.area,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Text(
                    "${location.city}, ${location.state}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray
                )
            }
        }

        // Accuracy overlay (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                "±${location.accuracy}",
                style = MaterialTheme.typography.labelSmall,
                color = DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PhoneNumberBanner(number: String) {
    val formatted = if (number.length == 10)
        "+91 ${number.substring(0, 5)} ${number.substring(5)}"
    else "+91 $number"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(BlueDark, BluePrimary, BlueAccent)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🇮🇳", fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Tracked Number",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    formatted,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Icon(Icons.Default.PhoneAndroid, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun LocationDetailCard(location: PhoneLocation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = BluePrimary, modifier = Modifier.size(22.dp))
                Text("Current Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            LocationRow(Icons.Default.Place, "Area", location.area, BluePrimary)
            Spacer(modifier = Modifier.height(10.dp))
            LocationRow(Icons.Default.LocationCity, "City", location.city)
            Spacer(modifier = Modifier.height(10.dp))
            LocationRow(Icons.Default.Map, "State", location.state)
            Spacer(modifier = Modifier.height(10.dp))
            LocationRow(Icons.Default.MarkunreadMailbox, "PIN Code", location.pincode)
            Spacer(modifier = Modifier.height(10.dp))
            LocationRow(Icons.Default.Flag, "Country", "India 🇮🇳")
            Spacer(modifier = Modifier.height(10.dp))
            LocationRow(
                Icons.Default.AccessTime, "Last Updated", location.lastUpdated,
                if (location.isLive) GreenOnline else OrangeWarning
            )
        }
    }
}

@Composable
private fun LocationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, null, tint = MediumGray, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun DeviceInfoCard(location: PhoneLocation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Smartphone, null, tint = TealAccent, modifier = Modifier.size(22.dp))
                Text("Network & Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceInfoItem(Icons.Default.SignalCellularAlt, "Operator", location.operator, BlueAccent, Modifier.weight(1f))
                DeviceInfoItem(Icons.Default.Wifi, "Network", location.networkType, GreenOnline, Modifier.weight(1f))
                DeviceInfoItem(
                    Icons.Default.BatteryStd, "Battery", "${location.batteryLevel}%",
                    when {
                        location.batteryLevel > 50 -> GreenOnline
                        location.batteryLevel > 20 -> OrangeWarning
                        else -> RedEmergency
                    },
                    Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Battery Level", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${location.batteryLevel}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            location.batteryLevel > 50 -> GreenOnline
                            location.batteryLevel > 20 -> OrangeWarning
                            else -> RedEmergency
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { location.batteryLevel / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = when {
                        location.batteryLevel > 50 -> GreenOnline
                        location.batteryLevel > 20 -> OrangeWarning
                        else -> RedEmergency
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CoordinatesCard(location: PhoneLocation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlueLight),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BluePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.GpsFixed, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("GPS Coordinates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Lat: ${"%.4f".format(location.latitude)}°N", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                Text("Lng: ${"%.4f".format(location.longitude)}°E", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Accuracy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(location.accuracy, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
            }
        }
    }
}

@Composable
private fun ActionButtons(onBack: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenOnline)
        ) {
            Icon(Icons.Default.Refresh, null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Location", color = Color.White, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, BluePrimary)
        ) {
            Icon(Icons.Default.Search, null, tint = BluePrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Track Another Number", color = BluePrimary, fontWeight = FontWeight.Bold)
        }
    }
}
