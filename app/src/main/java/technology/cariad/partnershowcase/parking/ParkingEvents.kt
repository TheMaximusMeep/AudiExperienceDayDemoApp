package technology.cariad.partnershowcase.parking

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ParkingEvent(
    val atMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val sender: String?,
) {
    val hasPosition: Boolean
        get() = latitude != null && longitude != null
}

/** A parking session the driver started from our offer. */
data class ParkingSession(val startedAtMillis: Long, val zone: String)

/**
 * The last "car parked" event and the running session. Kept on disk, because the event usually
 * arrives while our app is not on screen — often not even running.
 */
object ParkingEvents {
    private val _latest = MutableStateFlow<ParkingEvent?>(null)
    val latest: StateFlow<ParkingEvent?> = _latest.asStateFlow()

    private val _session = MutableStateFlow<ParkingSession?>(null)
    val session: StateFlow<ParkingSession?> = _session.asStateFlow()

    fun load(context: Context) {
        val prefs = prefs(context)
        val at = prefs.getLong("at", 0L)
        _latest.value =
            if (at == 0L) null
            else
                ParkingEvent(
                    atMillis = at,
                    latitude = prefs.getString("lat", null)?.toDoubleOrNull(),
                    longitude = prefs.getString("lon", null)?.toDoubleOrNull(),
                    sender = prefs.getString("sender", null),
                )
        val started = prefs.getLong("session", 0L)
        _session.value = if (started == 0L) null else ParkingSession(started, ZONE)
    }

    fun record(context: Context, event: ParkingEvent) {
        prefs(context)
            .edit()
            .putLong("at", event.atMillis)
            .putString("lat", event.latitude?.toString())
            .putString("lon", event.longitude?.toString())
            .putString("sender", event.sender)
            .putLong("session", 0L)
            .apply()
        _latest.value = event
        _session.value = null
    }

    fun startSession(context: Context) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong("session", now).apply()
        _session.value = ParkingSession(now, ZONE)
    }

    fun stopSession(context: Context) {
        prefs(context).edit().putLong("session", 0L).apply()
        _session.value = null
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        _latest.value = null
        _session.value = null
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("parking", Context.MODE_PRIVATE)

    /** A made-up parking zone, so the offer reads like a real one. */
    private const val ZONE = "Zone 4711 · Audi Forum"
}
