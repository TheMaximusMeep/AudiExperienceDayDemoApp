package technology.cariad.partnershowcase.parking

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * The answer to "car parked": we ask the App Store to show a notification for us.
 *
 * The store shows it only for apps that declare the parking receiver and request
 * `com.example.carstore.permission.SEND_PARKING_BROADCAST`, so no other app can borrow its voice.
 */
object StoreNotification {

    private const val ACTION_CAR_PARKED_RESPONSE =
        "com.example.carstore.action.CAR_PARKED_RESPONSE"

    fun offerParkingSession(context: Context, storePackage: String) {
        val icon = iconPng(context) ?: return
        val intent =
            Intent(ACTION_CAR_PARKED_RESPONSE).apply {
                setPackage(storePackage)
                putExtra("sender_package", context.packageName)
                putExtra("notification_title", "Partner Showcase: start parking here?")
                putExtra("notification_message", "Zone 4711 · Audi Forum · tap to start")
                putExtra("notification_icon", icon)
            }
        Log.i("PartnerShowcase", "Asking $storePackage to show our parking offer")
        context.sendBroadcast(intent)
    }

    private fun iconPng(context: Context): ByteArray? =
        try {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 192, 192)
            drawable.draw(Canvas(bitmap))
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.w("PartnerShowcase", "Could not render the icon for the store notification", e)
            null
        }
}
