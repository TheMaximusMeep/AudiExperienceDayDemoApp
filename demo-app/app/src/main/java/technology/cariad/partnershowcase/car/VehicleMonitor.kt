package technology.cariad.partnershowcase.car

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One vehicle signal (VHAL property) as the app sees it. */
data class Signal(val name: String, val id: Int, val value: String, val mode: String, val areas: Int)

/** What the car tells us right now. `null` = the car did not say (yet), or we may not ask. */
data class VehicleSnapshot(
    val connected: Boolean = false,
    val gear: Int? = null,
    val speedMetersPerSecond: Float? = null,
    val parkingBrakeOn: Boolean? = null,
    val ux: UxState = UxState(),
)

/**
 * The car's driver-distraction rules for the current driving state.
 *
 * Apps do not decide themselves whether the car is "driving" — the car does, per market, and hands
 * the result over as a set of restrictions to follow.
 */
data class UxState(
    val requiresDistractionOptimization: Boolean = false,
    val activeRestrictions: Int = CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
    val maxStringLength: Int = Int.MAX_VALUE,
    val maxContentItems: Int = Int.MAX_VALUE,
) {
    fun has(restriction: Int) = activeRestrictions and restriction != 0
}

/**
 * Connects to the car service and turns callbacks into one observable [state].
 *
 * Three APIs, all part of Android Automotive and available to every app in the store:
 * - `CarPropertyManager` for vehicle properties (gear, speed, parking brake),
 * - `CarUxRestrictionsManager` for the driver-distraction rules,
 * - `Car` itself, which reconnects on its own when the car service restarts.
 */
class VehicleMonitor(private val context: Context) {

    private val _state = MutableStateFlow(VehicleSnapshot())
    val state: StateFlow<VehicleSnapshot> = _state.asStateFlow()

    private var car: Car? = null
    private var properties: CarPropertyManager? = null
    private var uxManager: CarUxRestrictionsManager? = null

    private val propertyCallback =
        object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                when (value.propertyId) {
                    VehiclePropertyIds.GEAR_SELECTION ->
                        _state.update { it.copy(gear = value.value as? Int) }
                    VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                        _state.update { it.copy(speedMetersPerSecond = value.value as? Float) }
                    VehiclePropertyIds.PARKING_BRAKE_ON ->
                        _state.update { it.copy(parkingBrakeOn = value.value as? Boolean) }
                }
            }

            override fun onErrorEvent(propertyId: Int, zone: Int) {
                Log.w(TAG, "Property ${VehiclePropertyIds.toString(propertyId)} reported an error")
            }
        }

    private val uxListener =
        CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
            _state.update { it.copy(ux = restrictions.toUxState()) }
        }

    fun start() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        car =
            Car.createCar(context, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (ready) onCarReady(connectedCar) else onCarLost()
            }
    }

    fun stop() {
        onCarLost()
        car?.disconnect()
        car = null
    }

    /**
     * Every vehicle signal this app may read, with its current value. The car only lists what the
     * app's permissions allow — ask for more permissions and the list grows.
     */
    fun signals(): List<Signal> {
        val manager = properties ?: return emptyList()
        return manager.propertyList
            .map { config ->
                val area = config.areaIds.firstOrNull() ?: 0
                val readable =
                    config.access == CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ ||
                        config.access == CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_READ_WRITE
                val value =
                    if (!readable) "write only"
                    else
                        try {
                            format(manager.getProperty<Any>(config.propertyId, area)?.value)
                        } catch (e: Exception) {
                            "not available"
                        }
                Signal(
                    name = VehiclePropertyIds.toString(config.propertyId),
                    id = config.propertyId,
                    value = value,
                    mode =
                        when (config.changeMode) {
                            CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_STATIC -> "static"
                            CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_ONCHANGE -> "on change"
                            else -> "continuous"
                        },
                    areas = config.areaIds.size,
                )
            }
            .sortedBy { it.name }
    }

    private fun format(value: Any?): String =
        when (value) {
            null -> "–"
            is IntArray -> value.joinToString(", ", "[", "]")
            is FloatArray -> value.joinToString(", ", "[", "]")
            is LongArray -> value.joinToString(", ", "[", "]")
            is ByteArray -> "${value.size} bytes"
            is Array<*> -> value.joinToString(", ", "[", "]")
            is Float -> "%.2f".format(value)
            else -> value.toString()
        }.take(60)

    /** Speed needs a runtime permission; call again once the driver granted it. */
    fun subscribeSpeed() {
        subscribe(VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI)
    }

    private fun onCarReady(connectedCar: Car) {
        properties = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
        uxManager =
            connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager

        subscribe(VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        subscribe(VehiclePropertyIds.PARKING_BRAKE_ON, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        subscribeSpeed()

        uxManager?.registerListener(uxListener)
        val current = uxManager?.currentCarUxRestrictions?.toUxState() ?: UxState()
        _state.update { it.copy(connected = true, ux = current) }
    }

    private fun onCarLost() {
        @Suppress("DEPRECATION") properties?.unregisterCallback(propertyCallback)
        uxManager?.unregisterListener()
        properties = null
        uxManager = null
        _state.update { it.copy(connected = false) }
    }

    @Suppress("DEPRECATION") // registerCallback: still the API every AAOS version in the field has.
    private fun subscribe(propertyId: Int, rate: Float) {
        val manager = properties ?: return
        try {
            manager.registerCallback(propertyCallback, propertyId, rate)
        } catch (e: SecurityException) {
            // Typical mistake: forgetting that vehicle data is permission-protected like location.
            Log.i(TAG, "No permission for ${VehiclePropertyIds.toString(propertyId)} (yet)")
        } catch (e: IllegalArgumentException) {
            // Not every car offers every property. Never assume one exists.
            Log.i(TAG, "${VehiclePropertyIds.toString(propertyId)} is not supported by this car")
        }
    }

    private fun CarUxRestrictions.toUxState() =
        UxState(
            requiresDistractionOptimization = isRequiresDistractionOptimization,
            activeRestrictions = activeRestrictions,
            maxStringLength = maxRestrictedStringLength,
            maxContentItems = maxCumulativeContentItems,
        )

    private companion object {
        const val TAG = "PartnerShowcase"
    }
}

/** Gear values of the vehicle HAL (VehicleGear), as the driver reads them. */
fun gearLabel(gear: Int?): String =
    when (gear) {
        null, 0 -> "–"
        1 -> "N"
        2 -> "R"
        4 -> "P"
        8 -> "D"
        else -> {
            // GEAR_1 = 0x10, GEAR_2 = 0x20, … : a manual or tiptronic gear.
            val index = Integer.numberOfTrailingZeros(gear) - 3
            if (index in 1..9) "$index" else "?"
        }
    }

const val GEAR_PARK = 4
