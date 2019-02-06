package com.oth.wifi.fetch

interface UrlOverNetworkListener {
    fun onResponse(result: String)
    fun onTimeout()
    fun onNotConnectedToNetwork()
    fun onError(e: String?)
}