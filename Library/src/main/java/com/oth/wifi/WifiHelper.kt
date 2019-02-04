package com.oth.wifi

import android.app.Activity
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.oth.wifi.connect.WifiConnectHelper
import com.oth.wifi.connect.WifiConnectListener
import com.oth.wifi.fetch.UrlOverNetworkListener
import com.oth.wifi.misc.Utils
import com.oth.wifi.misc.log
import com.oth.wifi.scan.SsidAvailableListener
import com.oth.wifi.scan.WifiScanHelper
import com.oth.wifi.scan.WifiScanListener
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL


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

    fun fetchAsync(context: Activity, url: String, timeout: Int, urlOverWifiListener: UrlOverNetworkListener) {
        log( "fetchAsync")

        if (getCurrentNetworkInfo(context)?.ssid == null) {
            context.runOnUiThread { urlOverWifiListener.onNotConnectedToWifi() }
            return
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val req = NetworkRequest.Builder()
        req.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

        try {
            cm.requestNetwork(req.build(), object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {

                    Thread(Runnable {
                        var result: String?

                        // 0 = all good
                        // 1 = timeout
                        // 2 = exception
                        var errorType = 0

                        log("onAvailable")

                        val url = URL(url)
                        val urlConnection = network.openConnection(url) as HttpURLConnection
                        urlConnection.connectTimeout = timeout
                        urlConnection.readTimeout = timeout

                        //
                        try {
                            val ins = BufferedInputStream(urlConnection.inputStream)
                            result = Utils.readStream(ins)

                            log(result)
                        } catch (e: Exception) {
                            log("Exception")

                            e.printStackTrace()

                            errorType = if (e is SocketTimeoutException) {
                                1
                            } else {
                                2
                            }
                            result = e.toString()

                        } finally {
                            urlConnection.disconnect()
                        }


                        log("result: $result")

                        when (errorType) {
                            1 -> context.runOnUiThread { urlOverWifiListener.onTimeout() }
                            2 -> context.runOnUiThread { urlOverWifiListener.onError(result) }
                            else -> context.runOnUiThread { urlOverWifiListener.onResponse(result!!) }
                        }
                    }).start()
                }
            })
        } catch (e: SecurityException) {
            context.runOnUiThread { urlOverWifiListener.onError(e.message) }
        }
    }
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////

    fun forceWifiUsage(context: Context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//            manager.bindProcessToNetwork(null)
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            ConnectivityManager.setProcessDefaultNetwork(null)
//        }

        // Add any NetworkCapabilities.NET_CAPABILITY_...
        val capabilities = intArrayOf(NetworkCapabilities.NET_CAPABILITY_INTERNET)

// Add any NetworkCapabilities.TRANSPORT_...
        val transportTypes = intArrayOf(NetworkCapabilities.TRANSPORT_WIFI)

        alwaysPreferNetworksWith(context, capabilities, transportTypes)

    }

    private fun alwaysPreferNetworksWith(context: Context, capabilities: IntArray, transportTypes: IntArray) {

        val request = NetworkRequest.Builder()

        // add capabilities
        for (cap in capabilities) {
            request.addCapability(cap)
        }

        // add transport types
        for (trans in transportTypes) {
            request.addTransportType(trans)
        }

        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.registerNetworkCallback(request.build(), object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                try {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        ConnectivityManager.setProcessDefaultNetwork(network)
                    } else {
                        connectivityManager.bindProcessToNetwork(network)
                    }
                } catch (e: IllegalStateException) {
//                    Log.e("AAAAAAAA", "ConnectivityManager.NetworkCallback.onAvailable: ", e)
                }

            }
        })
    }
}
