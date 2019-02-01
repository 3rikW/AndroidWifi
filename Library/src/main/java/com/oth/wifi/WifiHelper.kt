package com.oth.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.oth.wifi.connect.WifiConnectHelper
import com.oth.wifi.connect.WifiConnectListener
import com.oth.wifi.scan.SsidAvailableListener
import com.oth.wifi.scan.WifiScanHelper
import com.oth.wifi.scan.WifiScanListener


object WifiHelper {

    val LIBRARY_VERSION = BuildConfig.VERSION_NAME

    fun getWifiManager(context: Context): WifiManager {
        return context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun getWifiState(context: Context) = WifiState.from(getWifiManager(context).isWifiEnabled)

    fun setWifiState(context: Context, wifiState: WifiState) {
        getWifiManager(context).isWifiEnabled = wifiState.state
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun scanWifiList(context: Context, wifiScanListener: WifiScanListener) {
        // enable wifi first -IMPORTANT-
        setWifiState(context, WifiState.WIFI_ENABLED)

        WifiScanHelper.scanWifi(context, wifiScanListener)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun isSsidAvailable(context: Context, ssid: String, ssidAvailableListener: SsidAvailableListener) {
        val wifiScanListener = object : WifiScanListener {
            override fun onWifiScanResults(results: List<ScanResult>) {
                ssidAvailableListener.onResult(results.firstOrNull { it.SSID.equals(ssid, false) } != null)
            }
        }

        scanWifiList(context, wifiScanListener)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun getCurrentNetworkInfo(context: Context): WifiInfo? {
        if (getWifiState(context) == WifiState.WIFI_DISABLED) return null
        return getWifiManager(context).connectionInfo
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun connectToWifi(context: Context, wifConnecListener: WifiConnectListener, wifiCredentials: WifiCredentials, timeOut: Long) {
        setWifiState(context, WifiState.WIFI_ENABLED)

        WifiConnectHelper.connectToWifi(context, wifConnecListener, wifiCredentials, timeOut)
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun forceWifiUsage(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            manager.bindProcessToNetwork(null)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager.setProcessDefaultNetwork(null)
        }
    }
}
