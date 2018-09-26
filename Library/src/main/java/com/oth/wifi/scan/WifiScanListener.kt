package com.oth.wifi.scan

import android.net.wifi.ScanResult

interface WifiScanListener {
    fun onWifiScanResults(results: List<ScanResult>)
}