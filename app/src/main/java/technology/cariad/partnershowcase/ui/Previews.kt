package technology.cariad.partnershowcase.ui

import android.car.drivingstate.CarUxRestrictions
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import technology.cariad.partnershowcase.audio.FocusState
import technology.cariad.partnershowcase.car.Signal
import technology.cariad.partnershowcase.car.UxState
import technology.cariad.partnershowcase.car.VehicleSnapshot
import technology.cariad.partnershowcase.net.NetworkState
import technology.cariad.partnershowcase.parking.ParkingEvent

// The screen in the states the live demo walks through, rendered by Android Studio without an
// emulator. Size: a wide, low car display (2220 × 816 px at 2x density).

private const val GEAR_PARK = 4
private const val GEAR_DRIVE = 8
private const val PREVIEW_TIME = 1_761_141_600_000L  // a fixed clock: previews must not change

private val NoActions = ShowcaseActions({}, {}, {}, {}, {}, {}, {}, {}, {})

private val Parked =
    VehicleSnapshot(connected = true, gear = GEAR_PARK, speedMetersPerSecond = 0f, parkingBrakeOn = true)

private val Driving =
    VehicleSnapshot(
        connected = true,
        gear = GEAR_DRIVE,
        speedMetersPerSecond = 13.9f,  // 50 km/h
        parkingBrakeOn = false,
        ux =
            UxState(
                requiresDistractionOptimization = true,
                activeRestrictions = CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED,
                maxStringLength = 120,
                maxContentItems = 21,
            ),
    )

private fun sample(
    vehicle: VehicleSnapshot,
    parkingEvent: ParkingEvent? = null,
    licence: LicenceState = LicenceState.NOT_ACTIVE,
    audio: FocusState = FocusState.PLAYING,
    network: NetworkState = NetworkState(online = true, wifi = false, metered = true),
) =
    ShowcaseState(
        app = AppInfo(versionName = "1.0.0", versionCode = 1, installedBy = "the preview"),
        vehicle = vehicle,
        speedAllowed = true,
        parkingEvent = parkingEvent,
        parkingSession = null,
        locationShared = true,
        licence = licence,
        audio = audio,
        network = network,
    )

@Preview(name = "Parked, by day", widthDp = 1110, heightDp = 408, showBackground = true)
@Composable
private fun ParkedPreview() = ShowcaseTheme { ShowcaseScreen(sample(Parked), NoActions) }

@Preview(
    name = "Driving, at night: fully restricted",
    widthDp = 1110,
    heightDp = 408,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DrivingPreview() = ShowcaseTheme { ShowcaseScreen(sample(Driving), NoActions) }

@Preview(name = "Parked: the store's \"car parked\" event, licence active", widthDp = 1110, heightDp = 408, showBackground = true)
@Composable
private fun ParkedEventPreview() =
    ShowcaseTheme {
        ShowcaseScreen(
            sample(
                Parked,
                parkingEvent =
                    ParkingEvent(
                        atMillis = PREVIEW_TIME,
                        latitude = 48.7836,
                        longitude = 11.4134,
                        sender = "com.example.carstore",
                    ),
                licence = LicenceState.ACTIVE,
            ),
            NoActions,
        )
    }

@Preview(name = "In a tunnel: offline, another app has the audio", widthDp = 1110, heightDp = 408, showBackground = true)
@Composable
private fun OfflinePreview() =
    ShowcaseTheme {
        ShowcaseScreen(
            sample(Parked, audio = FocusState.PAUSED, network = NetworkState(online = false)),
            NoActions,
        )
    }

@Preview(name = "Vehicle signals (parked only)", widthDp = 1110, heightDp = 408, showBackground = true)
@Composable
private fun SignalsPreview() =
    ShowcaseTheme {
        SignalsScreen(
            listOf(
                Signal("GEAR_SELECTION", 0x11400400, "P", "on change", 0),
                Signal("PERF_VEHICLE_SPEED", 0x11600207, "0.0 m/s", "continuous", 0),
                Signal("PARKING_BRAKE_ON", 0x11200402, "true", "on change", 0),
                Signal("NIGHT_MODE", 0x11200407, "false", "on change", 0),
                Signal("ENV_OUTSIDE_TEMPERATURE", 0x11600703, "22.0 °C", "continuous", 0),
                Signal("EV_CHARGE_PORT_OPEN", 0x1120030a, "false", "on change", 0),
            ),
            onClose = {},
        )
    }
