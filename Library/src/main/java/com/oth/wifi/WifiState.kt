package com.oth.wifi

enum class WifiState(val state: Boolean) {
    WIFI_ENABLED(state = true),
    WIFI_DISABLED(state = false);

    companion object {
        fun from(state: Boolean): WifiState {

            values().forEach {
                if (it.state == state) {
                    return it
                }
            }

            return WIFI_ENABLED
        }
    }
}