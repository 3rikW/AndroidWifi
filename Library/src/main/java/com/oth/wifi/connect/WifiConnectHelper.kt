package com.oth.wifi.connect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Handler
import android.text.TextUtils
import com.oth.wifi.WifiCredentials
import com.oth.wifi.WifiHelper
import com.oth.wifi.misc.Utils
import com.oth.wifi.misc.log
import com.oth.wifi.misc.removeQuotes


internal object WifiConnectHelper {

    private class WifiConnectReceiver : BroadcastReceiver() {
        var firstTime = true


        var wifConnectListener: WifiConnectListener? = null
        var expectedSsid = ""

        override fun onReceive(context: Context, intent: Intent) {
            // unregister receiver
            log("firstTime: $firstTime")
            if (firstTime) {
                firstTime = false
                return
            }

            val info: NetworkInfo? = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
            if (info != null && info.isConnected) {
                val ssid = WifiHelper.getCurrentNetworkInfo(context)?.ssid?.removeQuotes()//info.extraInfo.removeQuotes()
                log(" ssid: $ssid, expectedSsid: $expectedSsid, wifConnectListener: $wifConnectListener")

                // unregister receiver
                Utils.unregisterReceiver(context, this)

                // cancel handler timeout
                handler.removeCallbacksAndMessages(null)

                // callback
                if (expectedSsid == ssid) wifConnectListener?.onResult(true)
                else wifConnectListener?.onResult(false)

            }
        }
    }


    private val wifiConnectReceiver = WifiConnectReceiver()
    private val handler = Handler()

    fun connectToWifi(context: Context, wifConnectListener: WifiConnectListener, wifiCredentials: WifiCredentials, timeOut: Long) {
        Utils.unregisterReceiver(context, wifiConnectReceiver)

        // check if current wifi is the same as requested wifi - if true, return success directly
        if (WifiHelper.getCurrentNetworkInfo(context)?.ssid?.removeQuotes()?.equals(wifiCredentials.ssid, false) == true) {
            wifConnectListener.onResult(true)
            return
        }

        // otherwise: connect
        wifiConnectReceiver.wifConnectListener = wifConnectListener
        wifiConnectReceiver.expectedSsid = wifiCredentials.ssid
        wifiConnectReceiver.firstTime = true
        Utils.registerReceiver(context, wifiConnectReceiver, IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))

        connect(context, wifiCredentials.ssid, wifiCredentials.password)

        // manage timeout
        handler.postDelayed({
            Utils.unregisterReceiver(context, wifiConnectReceiver)

            wifConnectListener.onResult(false, true)

        }, timeOut)
    }


    fun connect(context: Context, ssid: String, password: String?) {

        val conf = WifiConfiguration()

        conf.SSID = String.format("\"%s\"", ssid)
        conf.status = WifiConfiguration.Status.ENABLED
        if (password.isNullOrBlank()) {
            conf.hiddenSSID = true
            conf.priority = 0xBADBAD
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        } else {
            conf.preSharedKey = String.format("\"%s\"", password)
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
        }

        val wifiManager = WifiHelper.getWifiManager(context)


        var netId = -1

        try {
            wifiManager.addNetwork(conf)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (netId == -1) {
            netId = getExistingNetworkId(context, ssid)
        }


        wifiManager.enableNetwork(netId, true)
        wifiManager.saveConfiguration()
        wifiManager.reconnect()
        wifiManager.reassociate()
    }


    fun getExistingNetworkId(context: Context, SSID: String): Int {
        val configuredNetworks = WifiHelper.getWifiManager(context).configuredNetworks
        if (configuredNetworks != null) {
            for (existingConfig in configuredNetworks) {
                if (trimQuotes(existingConfig.SSID) == trimQuotes(SSID)) {
                    return existingConfig.networkId
                }
            }
        }
        return -1
    }

    private fun trimQuotes(message: String): String {
        return if (TextUtils.isEmpty(message)) "" else message.replace("\"", "")
    }


}