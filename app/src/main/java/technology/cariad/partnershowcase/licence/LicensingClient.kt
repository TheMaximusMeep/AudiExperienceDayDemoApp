package technology.cariad.partnershowcase.licence

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The two answers the Entitlement SDK knows. There is no "unknown". */
object LicenseStatus {
    const val ACTIVE = 0
    const val NOT_ACTIVE = 1
}

data class License(val identifier: String, val status: Int)

fun interface LicenseResultListener {
    fun updateLicenseState(licenses: List<License>)
}

/**
 * Partner integration "licence sold by the car maker".
 *
 * The car's store sells the licence on the partner's behalf (one checkout, one bill, one account),
 * and the app asks the store on the device whether the vehicle holds it.
 *
 * **This is a stand-in with the exact shape of `VWLicensingClient` from the VW Entitlement SDK**,
 * so the demo runs on any emulator. In a real app the two calls read:
 *
 * ```
 * VWLicensingClient.getInstance(context).checkPackageLicense(packageName) { licenses -> … }
 * ```
 *
 * plus the runtime permission `com.example.carstore.permission.CHECK_LICENSE`.
 */
class LicensingClient private constructor(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** Asynchronous like the real SDK, which may take up to 15 s and then says NOT_ACTIVE. */
    fun checkPackageLicense(packageName: String, listener: LicenseResultListener) {
        executor.execute {
            Thread.sleep(ANSWER_DELAY_MS)
            val status = if (isActive(context)) LicenseStatus.ACTIVE else LicenseStatus.NOT_ACTIVE
            main.post { listener.updateLicenseState(listOf(License(packageName, status))) }
        }
    }

    companion object {
        private const val ANSWER_DELAY_MS = 900L

        @Volatile private var instance: LicensingClient? = null

        fun getInstance(context: Context): LicensingClient =
            instance
                ?: synchronized(this) {
                    instance ?: LicensingClient(context.applicationContext).also { instance = it }
                }

        // --- Demo plumbing: what the store's backend would decide ---------------------------

        private val _changes = MutableStateFlow(0)

        /** Ticks whenever the simulated store changes the licence, so the screen checks again. */
        val changes: StateFlow<Int> = _changes.asStateFlow()

        fun simulateStorePurchase(context: Context, active: Boolean) {
            prefs(context).edit().putBoolean("active", active).apply()
            _changes.value += 1
        }

        private fun isActive(context: Context) = prefs(context).getBoolean("active", false)

        private fun prefs(context: Context) =
            context.getSharedPreferences("licence", Context.MODE_PRIVATE)
    }
}
