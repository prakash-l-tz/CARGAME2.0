package com.example.myapplication.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.RecentSearch
import com.example.myapplication.data.isValidIndianMobile
import com.example.myapplication.ui.theme.*
import com.example.myapplication.viewmodel.*

@Composable
fun HomeScreen(
    viewModel: LocationViewModel,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onLocationFound: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.searchState) {
        if (uiState.searchState == SearchState.FOUND) onLocationFound()
    }

    // Share success dialog
    if (uiState.shareState == ShareState.SUCCESS) {
        ShareSuccessDialog(
            phone = uiState.sharePhone,
            onDismiss = { viewModel.resetShareState() }
        )
    }

    // Share bottom sheet
    if (showShareSheet) {
        ShareLocationSheet(
            sharePhone = uiState.sharePhone,
            isShareValid = uiState.isShareInputValid,
            shareState = uiState.shareState,
            hasPermission = hasLocationPermission,
            onPhoneChange = { viewModel.onSharePhoneChange(it) },
            onShare = {
                if (!hasLocationPermission) onRequestPermission()
                else viewModel.shareMyLocation(true)
            },
            onDismiss = {
                showShareSheet = false
                viewModel.resetShareState()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        HomeHeader(isDark = uiState.isDarkMode, onToggleDark = { viewModel.toggleDarkMode() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Search card
            SearchCard(
                phoneInput = uiState.phoneInput,
                inputError = uiState.inputError,
                isValid = uiState.isInputValid,
                isSearching = uiState.searchState == SearchState.SEARCHING,
                errorMessage = viewModel.getErrorMessage(uiState.inputError),
                onInputChange = { viewModel.onPhoneInputChange(it) },
                onSearch = {
                    keyboard?.hide()
                    viewModel.searchLocation()
                },
                onClear = { viewModel.clearSearch() }
            )

            // Not found / error message
            AnimatedVisibility(
                visible = uiState.searchState == SearchState.NOT_FOUND || uiState.searchState == SearchState.ERROR,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardOrangeLight)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, null, tint = OrangeWarning, modifier = Modifier.size(20.dp))
                        Text(
                            uiState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7B3F00)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share My Location button
            ShareMyLocationButton(onClick = { showShareSheet = true })

            Spacer(modifier = Modifier.height(20.dp))

            // How it works
            HowItWorksRow()

            Spacer(modifier = Modifier.height(20.dp))

            // Recent searches
            if (uiState.recentSearches.isNotEmpty()) {
                RecentSearchesSection(
                    searches = uiState.recentSearches,
                    onSearchClick = { phone ->
                        viewModel.selectRecent(phone)
                        keyboard?.hide()
                        viewModel.searchLocation()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // India only info
            InfoCard()
        }
    }
}

@Composable
private fun HomeHeader(isDark: Boolean, onToggleDark: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(BlueDark, BluePrimary)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onToggleDark) {
                    Icon(
                        if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        null, tint = Color.White
                    )
                }
            }

            val pulse by rememberInfiniteTransition(label = "p").animateFloat(
                0.93f, 1.05f,
                infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "sc"
            )
            Box(
                modifier = Modifier.size(86.dp).scale(pulse).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Live Location Finder", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Real-time GPS tracking via Firebase", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(0.15f))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🇮🇳", fontSize = 15.sp)
                Text("+91 India Only", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SearchCard(
    phoneInput: String,
    inputError: InputError,
    isValid: Boolean,
    isSearching: Boolean,
    errorMessage: String,
    onInputChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    val borderColor = when {
        isValid -> GreenOnline
        inputError != InputError.NONE && phoneInput.isNotEmpty() -> RedEmergency
        else -> MaterialTheme.colorScheme.outline.copy(0.4f)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Track a Number", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Enter 10-digit Indian mobile number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))

            // Input field
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🇮🇳", fontSize = 18.sp)
                    Text("+91", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BluePrimary)
                    Box(modifier = Modifier.width(1.dp).height(22.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                }

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { onInputChange(it.filter { c -> c.isDigit() }.take(10)) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text("XXXXX XXXXX", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), letterSpacing = 2.sp)
                    },
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                AnimatedVisibility(visible = phoneInput.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            null,
                            tint = if (isValid) GreenOnline else MediumGray
                        )
                    }
                }
            }

            // Validation message
            AnimatedVisibility(visible = phoneInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                when {
                    isValid -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = GreenOnline, modifier = Modifier.size(13.dp))
                        Text("Valid Indian number ✓", style = MaterialTheme.typography.labelSmall, color = GreenOnline, fontWeight = FontWeight.SemiBold)
                    }
                    inputError == InputError.INVALID_START -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Error, null, tint = RedEmergency, modifier = Modifier.size(13.dp))
                        Text(errorMessage, style = MaterialTheme.typography.labelSmall, color = RedEmergency)
                    }
                    else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Info, null, tint = OrangeWarning, modifier = Modifier.size(13.dp))
                        Text("${phoneInput.length}/10 digits", style = MaterialTheme.typography.labelSmall, color = OrangeWarning)
                    }
                }
            }

            // Digit dots progress
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(10) { i ->
                    Box(
                        modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(
                                animateColorAsState(
                                    if (i < phoneInput.length) (if (phoneInput.length == 10) GreenOnline else BlueAccent) else MaterialTheme.colorScheme.surfaceVariant,
                                    label = "d$i"
                                ).value
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track button
            Button(
                onClick = onSearch,
                enabled = isValid && !isSearching,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    disabledContainerColor = MediumGray.copy(0.25f)
                )
            ) {
                if (isSearching) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Finding location...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.GpsFixed, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Track Live Location", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun ShareMyLocationButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GreenOnline.copy(0.1f)),
        border = BorderStroke(1.5.dp, GreenOnline.copy(0.4f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(GreenOnline),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MyLocation, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Share My Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = GreenOnline)
                Text("Save your GPS to Firebase so others can track you", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = GreenOnline)
        }
    }
}

@Composable
private fun HowItWorksRow() {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text("How It Works", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepItem("1", Icons.Default.MyLocation, "Share GPS", "Enter your number\n+ share location", GreenOnline, Modifier.weight(1f))
            StepItem("2", Icons.Default.Storage, "Firebase\nStores It", "Saved securely\nreal-time DB", BlueAccent, Modifier.weight(1f))
            StepItem("3", Icons.Default.Search, "Track\nAny Number", "Enter number\nsee live on map", BluePrimary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepItem(
    step: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.07f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                Text(step, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RecentSearchesSection(searches: List<RecentSearch>, onSearchClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.History, null, tint = MediumGray, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        searches.forEach { s ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onSearchClick(s.phoneNumber) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(BlueContainer), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.History, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = BluePrimary, fontWeight = FontWeight.Bold)) { append("+91 ") }
                                append(if (s.phoneNumber.length == 10) "${s.phoneNumber.substring(0, 5)} ${s.phoneNumber.substring(5)}" else s.phoneNumber)
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(s.city, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(s.time, style = MaterialTheme.typography.labelSmall, color = MediumGray)
                    Icon(Icons.Default.ChevronRight, null, tint = MediumGray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBlueLight),
        border = BorderStroke(1.dp, BluePrimary.copy(0.2f))
    ) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Security, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
            Column {
                Text("Consent-based Tracking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BluePrimary)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    "Location is only visible when the person willingly shares it via this app. Indian numbers only (6/7/8/9 series).",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBlueContainer
                )
            }
        }
    }
}

// ─── Share Location Bottom Sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareLocationSheet(
    sharePhone: String,
    isShareValid: Boolean,
    shareState: ShareState,
    hasPermission: Boolean,
    onPhoneChange: (String) -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(GreenOnline), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MyLocation, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Share My Location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your GPS will be saved to Firebase under your number.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(20.dp))

            // Phone input
            OutlinedTextField(
                value = sharePhone,
                onValueChange = { onPhoneChange(it.filter { c -> c.isDigit() }.take(10)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your Mobile Number") },
                placeholder = { Text("Enter your 10-digit number") },
                leadingIcon = {
                    Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("🇮🇳", fontSize = 16.sp)
                        Text("+91", fontWeight = FontWeight.Bold, color = BluePrimary, fontSize = 14.sp)
                    }
                },
                trailingIcon = {
                    if (isShareValid) Icon(Icons.Default.CheckCircle, null, tint = GreenOnline)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (isShareValid) onShare() }),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                isError = sharePhone.length == 10 && !isShareValid
            )

            if (!hasPermission) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.LocationOff, null, tint = OrangeWarning, modifier = Modifier.size(14.dp))
                    Text("Location permission required", style = MaterialTheme.typography.labelSmall, color = OrangeWarning)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val busy = shareState == ShareState.GETTING_GPS || shareState == ShareState.SAVING
            Button(
                onClick = onShare,
                enabled = isShareValid && !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenOnline, disabledContainerColor = MediumGray.copy(0.3f))
            ) {
                if (busy) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (shareState == ShareState.GETTING_GPS) "Getting GPS..." else "Saving...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Upload, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save My Live Location", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ShareSuccessDialog(phone: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CheckCircle, null, tint = GreenOnline, modifier = Modifier.size(40.dp)) },
        title = { Text("Location Shared!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your live GPS location has been saved to Firebase for:", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "+91 ${if (phone.length == 10) "${phone.substring(0, 5)} ${phone.substring(5)}" else phone}",
                    fontWeight = FontWeight.ExtraBold,
                    color = BluePrimary,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Anyone can now track your location by entering this number.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) { Text("Done") }
        }
    )
}
