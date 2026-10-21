package technology.cariad.partnershowcase.licence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Demo only: plays the store's backend granting or revoking the licence.
 *
 * `adb shell am broadcast -a technology.cariad.partnershowcase.DEMO_LICENCE
 *  -p technology.cariad.partnershowcase --ez active true`
 */
class DemoLicenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LicensingClient.simulateStorePurchase(context, intent.getBooleanExtra("active", true))
    }
}
