package com.oth.wifi.connect

interface WifiConnectListener {
    /**
     * if (connected) = OK
     * if (!connected && !timeout) = wifi connected to a different ssid
     * if (!connected && timeout) = timeout
     */
    fun onResult(connected: Boolean, timeout: Boolean = false)
}