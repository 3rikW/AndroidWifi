package com.oth.wifi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.oth.wifi.connect.WifiConnectHelper
import com.oth.wifi.connect.WifiConnectListener
import com.oth.wifi.misc.Utils
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

    fun forceWifiUsage(context: Activity, useWifi: Boolean) {
        var canWriteFlag = false

        if (useWifi) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    canWriteFlag = Settings.System.canWrite(context)

                    if (!canWriteFlag) {
                        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                        intent.data = Uri.parse("package:" + context.packageName)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        Utils.startActivityIntent(context, intent)
                    }

                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canWriteFlag || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    val manager = context
                            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val builder: NetworkRequest.Builder = NetworkRequest.Builder()
                    //set the transport type do WIFI
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)


                    manager.requestNetwork(builder.build(), object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                manager.bindProcessToNetwork(network)
                            } else {
                                //This method was deprecated in API level 23
                                ConnectivityManager.setProcessDefaultNetwork(network)
                            }
                            try {
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            manager.unregisterNetworkCallback(this)
                        }
                    })
                }
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val manager = context
                        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                manager.bindProcessToNetwork(null)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ConnectivityManager.setProcessDefaultNetwork(null)
            }
        }
    }
}
