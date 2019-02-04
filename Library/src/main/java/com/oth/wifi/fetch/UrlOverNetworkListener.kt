package com.oth.wifi.fetch

interface UrlOverNetworkListener {
    fun onResponse(result: String)
    fun onTimeout()
    fun onNotConnectedToWifi()
    fun onError(e: String?)
}