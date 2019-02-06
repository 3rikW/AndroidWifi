package com.oth.wifi.fetch

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class TransportType(val type: Int, val connectivity: Int) {
    TRANSPORT_WIFI(NetworkCapabilities.TRANSPORT_WIFI, ConnectivityManager.TYPE_WIFI),
    TRANSPORT_CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR, ConnectivityManager.TYPE_MOBILE),
}