package technology.cariad.partnershowcase

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import technology.cariad.partnershowcase.audio.AudioFocus
import technology.cariad.partnershowcase.car.Signal
import technology.cariad.partnershowcase.car.VehicleMonitor
import technology.cariad.partnershowcase.ui.SignalsScreen
import technology.cariad.partnershowcase.net.NetworkMonitor
import technology.cariad.partnershowcase.licence.LicenseStatus
import technology.cariad.partnershowcase.licence.LicensingClient
import technology.cariad.partnershowcase.parking.ParkingEvents
import technology.cariad.partnershowcase.ui.AppInfo
import technology.cariad.partnershowcase.ui.LicenceState
import technology.cariad.partnershowcase.ui.ShowcaseActions
import technology.cariad.partnershowcase.ui.ShowcaseScreen
import technology.cariad.partnershowcase.ui.ShowcaseState
import technology.cariad.partnershowcase.ui.ShowcaseTheme

class MainActivity : ComponentActivity() {

    private lateinit var vehicle: VehicleMonitor
    private lateinit var network: NetworkMonitor
    private lateinit var audioFocus: AudioFocus

    private val resumed = mutableStateOf(false)
    private val signals = mutableStateOf<List<Signal>?>(null)
    private val speedAllowed = mutableStateOf(false)
    private val locationShared = mutableStateOf(false)
    private val licence = mutableStateOf(LicenceState.CHECKING)

    // Vehicle speed is a runtime permission, like location on a phone.
    private val speedPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            speedAllowed.value = granted
            if (granted) vehicle.subscribeSpeed()
        }

    // Fine location first; "Allow all the time" is a second, separate step since Android 11.
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshLocation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ParkingEvents.load(this)

        vehicle = VehicleMonitor(this).also { it.start() }
        network = NetworkMonitor(this).also { it.start() }
        audioFocus = AudioFocus(this)
        speedAllowed.value = isGranted(SPEED)

        val app = appInfo()
        val actions =
            ShowcaseActions(
                // Asked when the driver taps the speed — not as a wall of prompts at first start.
                allowSpeed = { speedPermission.launch(SPEED) },
                showSignals = { signals.value = vehicle.signals() },
                startParking = { ParkingEvents.startSession(this) },
                stopParking = { ParkingEvents.clear(this) },
                dismissParking = { ParkingEvents.clear(this) },
                allowLocation = ::askForLocation,
                unlockLicence = {
                    licence.value = LicenceState.CHECKING
                    // In the car this opens the store's checkout; here the store "sells" at once.
                    LicensingClient.simulateStorePurchase(this, active = true)
                },
                checkLicence = ::checkLicence,
                resetLicence = { LicensingClient.simulateStorePurchase(this, active = false) },
            )

        setContent {
            val snapshot by vehicle.state.collectAsState()
            val event by ParkingEvents.latest.collectAsState()
            val session by ParkingEvents.session.collectAsState()
            val licenceChanges by LicensingClient.changes.collectAsState()
            val focus by audioFocus.state.collectAsState()
            val net by network.state.collectAsState()
            LaunchedEffect(licenceChanges) { checkLicence() }

            // The trailer wants to play whenever video is allowed and we are on screen: ask the
            // car for audio focus first, and give it back the moment we stop.
            val videoAllowed =
                !snapshot.ux.has(android.car.drivingstate.CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO)
            LaunchedEffect(videoAllowed, resumed.value) {
                if (videoAllowed && resumed.value) audioFocus.requestToPlay() else audioFocus.release()
            }

            // The signal list is a parked-only screen: it closes as soon as the car restricts.
            val restricted = snapshot.ux.requiresDistractionOptimization
            LaunchedEffect(restricted) { if (restricted) signals.value = null }
            val shownSignals = signals.value

            ShowcaseTheme {
                if (shownSignals != null) {
                    SignalsScreen(shownSignals, onClose = { signals.value = null })
                } else ShowcaseScreen(
                    ShowcaseState(
                        app = app,
                        vehicle = snapshot,
                        speedAllowed = speedAllowed.value,
                        parkingEvent = event,
                        parkingSession = session,
                        locationShared = locationShared.value,
                        licence = licence.value,
                        audio = focus,
                        network = net,
                    ),
                    actions,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed.value = true
        refreshLocation()
        ParkingEvents.load(this)
    }

    override fun onPause() {
        resumed.value = false
        super.onPause()
    }

    override fun onDestroy() {
        vehicle.stop()
        network.stop()
        audioFocus.release()
        super.onDestroy()
    }

    private fun checkLicence() {
        licence.value = LicenceState.CHECKING
        LicensingClient.getInstance(this).checkPackageLicense(packageName) { licenses ->
            val active = licenses.any { it.status == LicenseStatus.ACTIVE }
            licence.value = if (active) LicenceState.ACTIVE else LicenceState.NOT_ACTIVE
        }
    }

    private fun askForLocation() {
        when {
            !isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ->
                locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            !isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION) ->
                locationPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun refreshLocation() {
        locationShared.value =
            isGranted(Manifest.permission.ACCESS_FINE_LOCATION) &&
                isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun appInfo(): AppInfo {
        val info = packageManager.getPackageInfo(packageName, 0)
        val installer = packageManager.getInstallSourceInfo(packageName).installingPackageName
        return AppInfo(info.versionName ?: "?", info.longVersionCode, installerLabel(installer))
    }

    private fun isGranted(permission: String) =
        checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SPEED = "android.car.permission.CAR_SPEED"

        fun installerLabel(installer: String?) =
            when {
                installer == null -> "adb (sideloaded)"
                installer == "com.example.carstore" -> "the Application Store"
                installer == "com.android.vending" -> "Google Play"
                else -> installer
            }
    }
}
