package com.oth.wifi.sample

import android.net.wifi.ScanResult
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.oth.wifi.WifiCredentials
import com.oth.wifi.WifiHelper
import com.oth.wifi.WifiState
import com.oth.wifi.connect.WifiConnectListener
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
    }
}
