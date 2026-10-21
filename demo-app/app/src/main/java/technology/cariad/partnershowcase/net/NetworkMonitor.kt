package technology.cariad.partnershowcase.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NetworkState(
    val online: Boolean = false,
    val wifi: Boolean = false,
    val metered: Boolean = true,
) {
    val label: String
        get() =
            when {
                !online -> "Offline"
                wifi -> "Wi-Fi" + if (metered) " · metered" else ""
                else -> "Mobile" + if (metered) " · metered" else ""
            }
}

/**
 * Internet in a car is not a phone's internet: tunnels and garages cut it, the car switches
 * between its own connection and the customer's data plan or Wi-Fi, and some of it is metered.
 * So: listen instead of assuming, keep working offline, and leave big downloads for unmetered
 * networks.
 */
class NetworkMonitor(context: Context) {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    private val _state = MutableStateFlow(NetworkState())
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                _state.value =
                    NetworkState(
                        online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                        wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                        metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                    )
            }

            override fun onLost(network: Network) {
                _state.value = NetworkState(online = false)
            }
        }

    fun start() = connectivity.registerDefaultNetworkCallback(callback)

    fun stop() = connectivity.unregisterNetworkCallback(callback)
}
