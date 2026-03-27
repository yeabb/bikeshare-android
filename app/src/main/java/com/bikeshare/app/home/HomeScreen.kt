package com.bikeshare.app.home

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bikeshare.app.core.network.StationDto
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    name: String,
    onScanToUnlock: () -> Unit,
    onActiveRide: () -> Unit,
    onLogout: () -> Unit,
    onHistory: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val rideCount by viewModel.rideCount.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.navigateToRide.collect { onActiveRide() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawer(
                name = name,
                rideCount = rideCount,
                onHistory = {
                    scope.launch { drawerState.close() }
                    onHistory()
                },
                onLogout = onLogout,
            )
        },
    ) {
        val onOpenDrawer = { scope.launch { drawerState.open() } }

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    ProfileIconButton(
                        modifier = Modifier.align(Alignment.TopStart),
                        onClick = { onOpenDrawer() },
                    )
                }
            }

            is HomeUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadStations() }) { Text("Retry") }
                    }
                    ProfileIconButton(
                        modifier = Modifier.align(Alignment.TopStart),
                        onClick = { onOpenDrawer() },
                    )
                }
            }

            is HomeUiState.Success -> {
                MapContent(
                    stations = state.stations,
                    onOpenDrawer = { onOpenDrawer() },
                    onScanToUnlock = onScanToUnlock,
                )
            }
        }
    }
}

@Composable
private fun ProfileIconButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier.padding(top = 48.dp, start = 8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountCircle,
            contentDescription = "Open profile menu",
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ProfileDrawer(
    name: String,
    rideCount: Int,
    onHistory: () -> Unit,
    onLogout: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Greeting
            Text(
                "Hi $name",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.DirectionsBike,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$rideCount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Rides", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Membership promo card
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column {
                            Text(
                                "Ride more.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Pay less.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Icon(
                            Icons.Outlined.DirectionsBike,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    listOf(
                        "Unlimited free unlocks",
                        "Discounted ride rates",
                        "Priority support",
                    ).forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(benefit, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { /* placeholder */ },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Learn more")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Menu items
            DrawerMenuItem(icon = Icons.Outlined.History, label = "History", onClick = onHistory)
            DrawerMenuItem(icon = Icons.Outlined.Wallet, label = "Wallet", onClick = {})
            DrawerMenuItem(icon = Icons.Outlined.Shield, label = "Safety Center", onClick = {})
            DrawerMenuItem(icon = Icons.Outlined.HelpOutline, label = "Help", onClick = {})
            DrawerMenuItem(icon = Icons.Outlined.Settings, label = "Settings", onClick = {})

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            DrawerMenuItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                label = "Sign out",
                onClick = onLogout,
                tint = MaterialTheme.colorScheme.error,
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                "Bikeshare v1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, tint = tint) },
        label = {
            Text(
                label,
                color = if (tint == MaterialTheme.colorScheme.error) tint
                        else MaterialTheme.colorScheme.onSurface,
            )
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapContent(
    stations: List<StationDto>,
    onOpenDrawer: () -> Unit,
    onScanToUnlock: () -> Unit,
) {
    val context = LocalContext.current
    var selectedStation by remember { mutableStateOf<StationDto?>(null) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(9.03, 38.74), 13f)
    }
    var mapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded, userLocation) {
        if (!mapLoaded || stations.isEmpty()) return@LaunchedEffect
        if (userLocation != null) {
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(userLocation!!.latitude, userLocation!!.longitude), 14f
                )
            )
        } else {
            val bounds = LatLngBounds.Builder().apply {
                stations.forEach { include(LatLng(it.lat, it.lng)) }
            }.build()
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                        userLocation = location
                    }
                }
        }
    }

    LaunchedEffect(Unit) {
        locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = { mapLoaded = true },
            properties = MapProperties(isMyLocationEnabled = true),
        ) {
            stations.forEach { station ->
                Marker(
                    state = MarkerState(position = LatLng(station.lat, station.lng)),
                    icon = createStationMarkerBitmap(context, station.available_bikes),
                    onClick = {
                        selectedStation = station
                        true
                    },
                )
            }
        }

        // Profile icon — top left
        ProfileIconButton(
            modifier = Modifier.align(Alignment.TopStart),
            onClick = onOpenDrawer,
        )

        // Floating scan button — bottom center
        Button(
            onClick = onScanToUnlock,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth(0.6f),
        ) {
            Text("Scan to Unlock")
        }
    }

    if (selectedStation != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedStation = null },
            sheetState = bottomSheetState,
        ) {
            StationDetailSheet(
                station = selectedStation!!,
                userLocation = userLocation,
                onScanToUnlock = {
                    selectedStation = null
                    onScanToUnlock()
                },
            )
        }
    }
}

@Composable
private fun StationDetailSheet(
    station: StationDto,
    userLocation: Location?,
    onScanToUnlock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                station.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            val isActive = station.status == "ACTIVE"
            Text(
                text = if (isActive) "Active" else "Inactive",
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                station.lat, station.lng,
                results,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                formatDistance(results[0]),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatItem(value = station.available_bikes.toString(), label = "bikes available")
            StatItem(value = station.open_docks.toString(), label = "docks open for parking")
        }

        if (station.bike_ids.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Bikes at this station",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            station.bike_ids.forEach { bikeId ->
                Text(bikeId, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onScanToUnlock,
            modifier = Modifier.fillMaxWidth(),
            enabled = station.available_bikes > 0,
        ) {
            Text(if (station.available_bikes > 0) "Scan to Unlock" else "No Bikes Available")
        }
    }
}

private fun formatDistance(meters: Float): String {
    return if (meters < 1000f) "${meters.toInt()} m away"
    else "%.1f km away".format(meters / 1000f)
}

@Composable
private fun StatItem(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}
