package com.konbini.mdbpayment.utils

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class CommonUtil {
    companion object {
        fun hiddenSystemBars(window: Window) {
            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            // Configure the behavior of the hidden system bars.
            windowInsetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}