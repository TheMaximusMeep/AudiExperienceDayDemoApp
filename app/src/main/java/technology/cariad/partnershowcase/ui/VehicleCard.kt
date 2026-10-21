package technology.cariad.partnershowcase.ui

import android.os.Process
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import technology.cariad.partnershowcase.car.VehicleSnapshot
import technology.cariad.partnershowcase.car.gearLabel
import technology.cariad.partnershowcase.net.NetworkState

/** What a car knows that a phone does not — read through standard Android Automotive APIs. */
@Composable
fun VehicleCard(
    vehicle: VehicleSnapshot,
    speedAllowed: Boolean,
    network: NetworkState,
    onAllowSpeed: () -> Unit,
    onShowSignals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalShowcaseColors.current
    val config = LocalConfiguration.current
    ShowcaseCard("Vehicle context", tag = "Car API", modifier = modifier) {
        if (!vehicle.connected) {
            Text("Connecting to the car service…", style = MaterialTheme.typography.bodyMedium, color = c.muted)
        }
        val speed =
            when {
                // Speed counts as location data: the car asks the driver, like a phone would.
                !speedAllowed -> "Allow ›"
                // Typical mistake: trusting every value. Cars report "not available" in their own
                // way, here as a negative speed, so check the range before showing it.
                vehicle.speedMetersPerSecond == null || vehicle.speedMetersPerSecond < 0f -> "–"
                else -> "${(vehicle.speedMetersPerSecond * 3.6f).roundToInt()} km/h"
            }
        Pair2(
            { Readout("Gear", gearLabel(vehicle.gear), it) },
            {
                Readout(
                    "Speed",
                    speed,
                    if (speedAllowed) it else it.clickable(onClick = onAllowSpeed),
                    highlight = if (speedAllowed) null else c.accentText,
                )
            },
        )
        Pair2(
            {
                Readout(
                    "Restrictions",
                    if (vehicle.ux.requiresDistractionOptimization) "Active" else "None",
                    it,
                    highlight = if (vehicle.ux.requiresDistractionOptimization) c.driving else c.accentText,
                )
            },
            {
                Readout(
                    "Parking brake",
                    when (vehicle.parkingBrakeOn) {
                        true -> "On"
                        false -> "Off"
                        null -> "–"
                    },
                    it,
                )
            },
        )
        Pair2(
            { Readout("Light", if (isSystemInDarkTheme()) "Night" else "Day", it) },
            { Readout("Screen (dp)", "${config.screenWidthDp}×${config.screenHeightDp}", it) },
        )
        Pair2(
            { Readout("Driver profile", "User ${Process.myUserHandle().hashCode()}", it) },
            {
                Readout(
                    "Network",
                    network.label,
                    it,
                    highlight = if (network.online) null else c.warning,
                )
            },
        )
        // A long list is no reading for the driver: only when parked.
        if (vehicle.ux.requiresDistractionOptimization) {
            QuietButton("All vehicle signals: when parked", {}, Modifier.fillMaxWidth())
        } else {
            QuietButton("All vehicle signals ›", onShowSignals, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Pair2(left: @Composable (Modifier) -> Unit, right: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        left(Modifier.weight(1f))
        right(Modifier.weight(1f))
    }
}
