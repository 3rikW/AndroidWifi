package com.oth.wifi.scan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import com.oth.wifi.WifiHelper
import com.oth.wifi.misc.Utils


internal object WifiScanHelper {

    private class ScanReceiver : BroadcastReceiver() {
        var wifiScanListener: WifiScanListener? = null

        override fun onReceive(context: Context, intent: Intent) {
            // unregister receiver
            Utils.unregisterReceiver(context, this)


            // return list
            wifiScanListener?.let {
                val results = WifiHelper.getWifiManager(context).scanResults
                it.onWifiScanResults(results)
            }
        }
    }


    private val scanReceiver = ScanReceiver()

    fun scanWifi(context: Context, wifiScanListener: WifiScanListener) {
        Utils.unregisterReceiver(context, scanReceiver)

        scanReceiver.wifiScanListener = wifiScanListener
        Utils.registerReceiver(context, scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        WifiHelper.getWifiManager(context).startScan()
    }


}