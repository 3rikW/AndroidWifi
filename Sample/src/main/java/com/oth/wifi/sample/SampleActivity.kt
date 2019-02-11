package com.oth.wifi.sample

import android.net.wifi.ScanResult
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.oth.wifi.WifiCredentials
import com.oth.wifi.WifiHelper
import com.oth.wifi.WifiState
import com.oth.wifi.connect.WifiConnectListener
import com.oth.wifi.fetch.TransportType
import com.oth.wifi.fetch.UrlOverNetworkListener
import com.oth.wifi.scan.SsidAvailableListener
import com.oth.wifi.scan.WifiScanListener
import kotlinx.android.synthetic.main.activity_sample.*

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)


        /////////////////////////////////////////////////////////////////

        wifiState.setOnClickListener {
            wifiStateText.text = WifiHelper.getWifiState(this@SampleActivity).name
        }

        toggleWifi.setOnClickListener {
            WifiHelper.setWifiState(this@SampleActivity, WifiState.from(!WifiHelper.getWifiState(this@SampleActivity).state))
        }

        /////////////////////////////////////////////////////////////////

        scan.setOnClickListener {
            val wifiScanListener = object : WifiScanListener {
                override fun onWifiScanResults(results: List<ScanResult>) {
                    scanText.text = "${results.size} networks found"
                }
            }

            WifiHelper.scanWifiList(this@SampleActivity, wifiScanListener)
        }

        /////////////////////////////////////////////////////////////////

        scanSsid.setOnClickListener {
            scanSsidText.text = "---"

            val listener = object : SsidAvailableListener {
                override fun onResult(available: Boolean) {
                    scanSsidText.text = "available: $available"
                }
            }

            WifiHelper.isSsidAvailable(this@SampleActivity, scanSsidEdit.text.toString(), listener)
        }

        /////////////////////////////////////////////////////////////////

        current.setOnClickListener {
            currentText.text = "ssid: ${WifiHelper.getCurrentNetworkInfo(this@SampleActivity)?.ssid}"
        }

        /////////////////////////////////////////////////////////////////

        connectSsid.setOnClickListener {
            connectSsidText.text = "---"


            val listener = object : WifiConnectListener {
                override fun onResult(connected: Boolean, timeout: Boolean) {
                    connectSsidText.text = "connected: $connected, timeout: $timeout"
                }
            }

            WifiHelper.connectToWifi(this@SampleActivity, listener, WifiCredentials(connectSsidSsid.text.toString(), connectSsidPass.text.toString()), connectSsidTimeout.text.toString().toLong())
        }

        /////////////////////////////////////////////////////////////////

        forgetSsid.setOnClickListener {
            WifiHelper.forgetWifi(this@SampleActivity, forgetSsidSsid.text.toString())
        }

        /////////////////////////////////////////////////////////////////

        forceWifi.setOnClickListener {
            WifiHelper.forceWifiUsage(this@SampleActivity)
        }

        /////////////////////////////////////////////////////////////////

        val listener = object : UrlOverNetworkListener {
            override fun onNotConnectedToNetwork() {
                Log.e("MainActivity", "onNotConnectedToWifi")
                fetchWifiText.text = "onNotConnectedToWifi"
            }

            override fun onResponse(result: String) {
                Log.e("MainActivity", "result: $result")
                fetchWifiText.text = "OK"
            }

            override fun onTimeout() {
                Log.e("MainActivity", "onTimeout")
                fetchWifiText.text = "onTimeout"
            }

            override fun onError(e: String?) {
                Log.e("MainActivity", "e: $e")
                fetchWifiText.text = "Exception / ERROR: $e"
            }
        }

        fetchWifi.setOnClickListener {
            fetchWifiText.text = "---"

            for (i in 0..50) {
                WifiHelper.fetchAsync(this@SampleActivity, "http://google.com", 5000, TransportType.TRANSPORT_WIFI, listener)
            }
        }

        fetchMobileData.setOnClickListener {
            fetchMobileDataText.text = "---"

            WifiHelper.fetchAsync(this@SampleActivity, "http://google.com", 5000, TransportType.TRANSPORT_CELLULAR, object : UrlOverNetworkListener {
                override fun onNotConnectedToNetwork() {
                    Log.e("MainActivity", "onNotConnectedToMobileData")
                    fetchMobileDataText.text = "onNotConnectedToMobileData"
                }

                override fun onResponse(result: String) {
                    Log.e("MainActivity", "result: $result")
                    fetchMobileDataText.text = "OK"
                }

                override fun onTimeout() {
                    Log.e("MainActivity", "onTimeout")
                    fetchMobileDataText.text = "onTimeout"
                }

                override fun onError(e: String?) {
                    Log.e("MainActivity", "e: $e")
                    fetchMobileDataText.text = "Exception / ERROR: $e"
                }
            })
        }
    }
}
