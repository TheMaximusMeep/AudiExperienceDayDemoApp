package technology.cariad.partnershowcase.parking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Partner integration "car parked".
 *
 * The App Store's CustomBroadcastService watches the gear. When the driver shifts to P after a
 * drive, it sends this broadcast to every app that declares a receiver for it — even if that app
 * is not running. A phone cannot do this: it does not know when you parked.
 *
 * Extras, all strings:
 * - `latitude`, `longitude`: only if this app holds fine *and* background location,
 * - `senderpackagename`: the store that sent it — the address for our answer.
 */
class CarParkedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CAR_PARKED) return

        val event =
            ParkingEvent(
                atMillis = System.currentTimeMillis(),
                latitude = intent.getStringExtra("latitude")?.toDoubleOrNull(),
                longitude = intent.getStringExtra("longitude")?.toDoubleOrNull(),
                sender = intent.getStringExtra("senderpackagename"),
            )
        Log.i(TAG, "Car parked: $event")
        ParkingEvents.record(context, event)

        // Ask the store to show "Start parking here?" on our behalf. The store's notification is
        // allowed to pop up while we are in the background; ours would need its own permission.
        event.sender?.let { StoreNotification.offerParkingSession(context, it) }
    }

    companion object {
        const val ACTION_CAR_PARKED =
            "com.example.carstore.action.CAR_PARKED"
        private const val TAG = "PartnerShowcase"
    }
}
