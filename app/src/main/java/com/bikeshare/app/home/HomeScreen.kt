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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bikeshare.app.core.network.StationDto
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onLogout: () -> Unit, onScanToUnlock: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                    Button(onClick = { viewModel.loadStations() }) {
                        Text("Retry")
                    }
                }
                TextButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Log out")
                }
            }
        }

        is HomeUiState.Success -> {
            MapContent(
                stations = state.stations,
                onLogout = onLogout,
                onScanToUnlock = onScanToUnlock,
            )
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapContent(stations: List<StationDto>, onLogout: () -> Unit, onScanToUnlock: () -> Unit) {
    val context = LocalContext.current
    var selectedStation by remember { mutableStateOf<StationDto?>(null) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    // Default fallback position (San Francisco, where simulator stations are)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(37.77, -122.41), 13f)
    }

    // Ask for location permission, then move camera to user's location if granted
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLocation = location
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 13f
                            )
                        )
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

        // Logout button — top-right overlay, for dev testing
        TextButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("Log out")
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
        // Station name + status badge
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

        // Distance from user
        if (userLocation != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                station.lat, station.lng,
                results,
            )
            val distanceText = formatDistance(results[0])
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                distanceText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats row
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            StatItem(value = station.available_bikes.toString(), label = "bikes available")
            StatItem(value = station.open_docks.toString(), label = "docks open for parking")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scan to unlock button
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
    val miles = meters / 1609.34f
    return if (miles < 0.1f) {
        "${(meters * 3.28084f).toInt()} ft away"
    } else {
        "%.1f mi away".format(miles)
    }
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
