package com.oth.wifi.misc

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

object Utils {

    fun registerReceiver(context: Context, receiver: BroadcastReceiver, filter: IntentFilter) {
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
        }
    }

    fun unregisterReceiver(context: Context, receiver: BroadcastReceiver) {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
        }

    }

    fun startActivityIntent(activity: Activity, intent: Intent) {
        activity.startActivity(intent)
    }
}