package com.oth.wifi

import android.app.Activity
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.oth.wifi.connect.WifiConnectHelper
import com.oth.wifi.connect.WifiConnectListener
import com.oth.wifi.fetch.TransportType
import com.oth.wifi.fetch.UrlOverNetworkListener
import com.oth.wifi.misc.Utils
import com.oth.wifi.scan.SsidAvailableListener
import com.oth.wifi.scan.WifiScanHelper
import com.oth.wifi.scan.WifiScanListener
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*


object WifiHelper {

    val LIBRARY_VERSION = BuildConfig.VERSION_NAME

    var networkInstanceCount = 0
    var networkInstanceStartTime = 0L
    var transportType: TransportType? = null

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

    fun forgetWifi(context: Context, ssid: String) {
        val netId = WifiConnectHelper.getExistingNetworkId(context, ssid)
        getWifiManager(context).removeNetwork(netId)
        getWifiManager(context).saveConfiguration()
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    fun fetchAsync(activity: Activity?, url: String, timeout: Int, transportType: TransportType, urlOverWifiListener: UrlOverNetworkListener) {
        Log.e("aaaaaaaaa", "fetchAsync")

        if (activity == null) {
            urlOverWifiListener.onContextError()
            return
        }

        val cm = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
        val rightConnectionType: Boolean = activeNetwork?.type == transportType.connectivity

        Log.e("aaaaaaaaa", "fetchAsync: isConnected: $isConnected, rightConnectionType: $rightConnectionType")

        /*
        we should not return here. If the user has LTE it will still fail since the activeNetwork will be 
        the Cellular one and it will not match wifi even though the phone is connected to both.
        
        if (!isConnected || !rightConnectionType) {
            activity.runOnUiThread { urlOverWifiListener.onNotConnectedToNetwork() }
            return
        }*/

        val req = NetworkRequest.Builder()
        req.addTransportType(transportType.type)

        try {

            Log.e("aaaaaaaaa", "fetchAsync")

            try {
                cm.requestNetwork(req.build(), object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(network: Network) {
                        Log.e("aaaaaaaaa", "fetchAsync: onAvailable")

                        cm.unregisterNetworkCallback(this)

                        this@WifiHelper.networkInstanceCount++
                        this@WifiHelper.transportType = transportType
                        if (this@WifiHelper.networkInstanceStartTime == 0L) {
                            this@WifiHelper.networkInstanceStartTime = Date().time
                        }

                        requestWithNetwork(activity, url, timeout, urlOverWifiListener, network)
                    }
                })
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(e.message + ", instance count: ${this@WifiHelper.networkInstanceCount}, " +
                        "time of first instance: ${this@WifiHelper.networkInstanceStartTime}, time of crash: ${Date().time}")
            }

        } catch (e: SecurityException) {
            activity.runOnUiThread { urlOverWifiListener.onError(e.message) }
        }
    }

    private fun requestWithNetwork(context: Activity, url: String, timeout: Int, urlOverWifiListener: UrlOverNetworkListener, network: Network) {
        Thread(Runnable {
            var result: String?

            // 0 = all good
            // 1 = timeout
            // 2 = exception
            var errorType = 0

            Log.e("aaaaaaaaa", "onAvailable")

            val url = URL(url)
            val urlConnection = network.openConnection(url) as HttpURLConnection
            urlConnection.connectTimeout = timeout
            urlConnection.readTimeout = timeout

            //
            try {
                val ins = BufferedInputStream(urlConnection.inputStream)
                result = Utils.readStream(ins)

                Log.e("aaaaaaaaa", result)
            } catch (e: Exception) {
                Log.e("aaaaaaaaa", "Exception")

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


            Log.e("aaaaaaaaa", "result: $result")

            when (errorType) {
                1 -> context.runOnUiThread { urlOverWifiListener.onTimeout() }
                2 -> context.runOnUiThread { urlOverWifiListener.onError(result) }
                else -> context.runOnUiThread { urlOverWifiListener.onResponse(result!!) }
            }
        }).start()
    }
//////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////

    fun forceWifiUsage(context: Context) {

        var canWriteFlag = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                canWriteFlag = Settings.System.canWrite(context)

                if (!canWriteFlag) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.setData(Uri.parse("package:" + context.packageName))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    Utils.startActivityIntent(context, intent)
                }
            }


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canWriteFlag || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val manager = context
                        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val builder: NetworkRequest.Builder
                builder = NetworkRequest.Builder()
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


////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
////            manager.bindProcessToNetwork(null)
////        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
////            ConnectivityManager.setProcessDefaultNetwork(null)
////        }
//
//        // Add any NetworkCapabilities.NET_CAPABILITY_...
//        val capabilities = intArrayOf(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//
//// Add any NetworkCapabilities.TRANSPORT_...
//        val transportTypes = intArrayOf(NetworkCapabilities.TRANSPORT_WIFI)
//
//        alwaysPreferNetworksWith(context, capabilities, transportTypes)

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
