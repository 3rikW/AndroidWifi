package com.oth.wifi.fetch

import android.net.NetworkCapabilities

enum class TransportType(val type: Int) {
    TRANSPORT_WIFI(NetworkCapabilities.TRANSPORT_WIFI),
    TRANSPORT_CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR),
}