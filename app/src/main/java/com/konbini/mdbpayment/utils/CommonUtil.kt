package com.konbini.mdbpayment.utils

import android.annotation.SuppressLint
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt

class CommonUtil {
    companion object {
        var language = "en"
        var country = "SG"
        @SuppressLint("ConstantLocale")
        var format: NumberFormat = NumberFormat.getCurrencyInstance(Locale(language, country))

        @SuppressLint("ConstantLocale")
        var currency: Currency = Currency.getInstance(Locale(language, country))

        fun formatCurrency(value: Float): String {
            var currency = 0F
            if (value > 0) currency = value
            return format.format(currency)
        }

        fun formatCurrency(value: Double): String {
            var currency = 0.00
            if (value > 0) currency = (value * 100.0).roundToInt() / 100.0
            return format.format(currency)
        }

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