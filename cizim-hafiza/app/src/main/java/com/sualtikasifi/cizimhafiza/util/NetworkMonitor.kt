package com.sualtikasifi.cizimhafiza.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the phone can actually reach the internet right now: the default network must have the internet capability AND
 * be validated by the system (a Wi-Fi with no uplink, or mobile data switched off, does not count). Everything that talks
 * to the server (online rooms, Quick Match, friends, league, daily challenge, rewarded ads, account) reads this.
 */
@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {

    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val _isOnline = MutableStateFlow(currentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        runCatching {
            manager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _isOnline.value = currentlyOnline() }
                // The default network is gone: until another one is announced (onAvailable) there is no connection. The
                // system's own "active network" can still report the dying one for a moment, so it is not asked here.
                override fun onLost(network: Network) { _isOnline.value = false }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            })
        }
    }

    private fun currentlyOnline(): Boolean {
        val m = manager ?: return true
        val caps = runCatching { m.getNetworkCapabilities(m.activeNetwork) }.getOrNull() ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
