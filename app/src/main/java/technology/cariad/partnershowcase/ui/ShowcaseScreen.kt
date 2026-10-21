package technology.cariad.partnershowcase.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import technology.cariad.partnershowcase.DemoConfig
import technology.cariad.partnershowcase.audio.FocusState
import technology.cariad.partnershowcase.car.VehicleSnapshot
import technology.cariad.partnershowcase.net.NetworkState
import technology.cariad.partnershowcase.parking.ParkingEvent
import technology.cariad.partnershowcase.parking.ParkingSession

data class AppInfo(val versionName: String, val versionCode: Long, val installedBy: String)

/** Everything the screen shows; MainActivity owns where it comes from. */
data class ShowcaseState(
    val app: AppInfo,
    val vehicle: VehicleSnapshot,
    val speedAllowed: Boolean,
    val parkingEvent: ParkingEvent?,
    val parkingSession: ParkingSession?,
    val locationShared: Boolean,
    val licence: LicenceState,
    val audio: FocusState,
    val network: NetworkState,
)

class ShowcaseActions(
    val allowSpeed: () -> Unit,
    val showSignals: () -> Unit,
    val startParking: () -> Unit,
    val stopParking: () -> Unit,
    val dismissParking: () -> Unit,
    val allowLocation: () -> Unit,
    val unlockLicence: () -> Unit,
    val checkLicence: () -> Unit,
    val resetLicence: () -> Unit,
)

@Composable
fun ShowcaseScreen(state: ShowcaseState, actions: ShowcaseActions) {
    val c = LocalShowcaseColors.current
    // Car screens are wide and low (e.g. 2220 × 816 on a panorama display). Cards keep their natural height and the
    // page scrolls — never cut content to fit, and never scroll on its own.
    BoxWithConstraints(Modifier.fillMaxSize().background(c.background)) {
        val columns =
            when {
                maxWidth >= 1800.dp -> 4
                maxWidth >= 1100.dp -> 3
                else -> 1
            }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header(state)

            val drive = @Composable { m: Modifier ->
                DriveAwareCard(state.vehicle.ux, state.audio, state.network, m)
            }
            val vehicle = @Composable { m: Modifier ->
                VehicleCard(
                    state.vehicle,
                    state.speedAllowed,
                    state.network,
                    actions.allowSpeed,
                    actions.showSignals,
                    m,
                )
            }
            val parking = @Composable { m: Modifier ->
                ParkingCard(
                    event = state.parkingEvent,
                    session = state.parkingSession,
                    locationShared = state.locationShared,
                    onStart = actions.startParking,
                    onStop = actions.stopParking,
                    onDismiss = actions.dismissParking,
                    onAllowLocation = actions.allowLocation,
                    modifier = m,
                )
            }
            val licence = @Composable { m: Modifier ->
                LicenceCard(state.licence, actions.unlockLicence, actions.checkLicence, actions.resetLicence, m)
            }

            val gap = Arrangement.spacedBy(20.dp)
            when (columns) {
                4 ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = gap) {
                        drive(Modifier.weight(1.2f))
                        vehicle(Modifier.weight(1f))
                        parking(Modifier.weight(1f))
                        licence(Modifier.weight(1f))
                    }
                3 ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = gap) {
                        drive(Modifier.weight(1.25f))
                        vehicle(Modifier.weight(1f))
                        Column(Modifier.weight(1f), verticalArrangement = gap) {
                            parking(Modifier.fillMaxWidth())
                            licence(Modifier.fillMaxWidth())
                        }
                    }
                else -> {
                    drive(Modifier.fillMaxWidth())
                    vehicle(Modifier.fillMaxWidth())
                    parking(Modifier.fillMaxWidth())
                    licence(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Header(state: ShowcaseState) {
    val c = LocalShowcaseColors.current
    val restricted = state.vehicle.ux.requiresDistractionOptimization
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                DemoConfig.GREETING,
                style = MaterialTheme.typography.displaySmall,
                color = c.accentText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Partner Showcase · v${state.app.versionName} (${state.app.versionCode}) · " +
                    "installed by ${state.app.installedBy}",
                style = MaterialTheme.typography.titleMedium,
                color = c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            if (restricted) "DRIVING" else "PARKED",
            style = MaterialTheme.typography.headlineSmall,
            color = if (restricted) Color.White else c.onAccent,
            modifier =
                Modifier.background(if (restricted) c.driving else c.accent, RoundedCornerShape(50))
                    .padding(horizontal = 28.dp, vertical = 10.dp),
        )
    }
}
