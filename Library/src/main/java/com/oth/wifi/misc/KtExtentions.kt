package com.oth.wifi.misc

import android.util.Log
import com.oth.wifi.BuildConfig

internal fun String.removeQuotes() = removePrefix("\"").removeSuffix("\"")

internal fun Any.log(message: Any?) {
    if (BuildConfig.LOGS_ENABLED) Log.v("class-${this.javaClass.simpleName}", "$message")
}

internal fun log(message: Any?) = "".log(message)