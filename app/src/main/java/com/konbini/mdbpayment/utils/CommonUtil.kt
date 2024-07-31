package com.konbini.mdbpayment.utils

import android.annotation.SuppressLint
import android.content.Intent
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.konbini.mdbpayment.MainApplication
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
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
            if (value > 0) currency = value
            return format.format(currency)
        }

        fun formatDateTime(pattern: String, time: Long): String {
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            val date = Date(time)
            return formatter.format(date)
        }

        fun hiddenSystemBars(window: Window) {
            val windowInsetsController =
                WindowCompat.getInsetsController(window, window.decorView)
            // Configure the behavior of the hidden system bars.
            windowInsetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }

        /**
         * Returns a list with all links contained in the input
         */
        fun extractUrls(text: String): MutableList<String> {
            val containedUrls: MutableList<String> = mutableListOf()
            val urlRegex =
                "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d\\s:#@%/;$()~_?\\+-=\\\\\\.&]*)"
            val pattern: Pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL)
            val urlMatcher: Matcher = pattern.matcher(text)
            while (urlMatcher.find()) {
                containedUrls.add(
                    text.substring(
                        urlMatcher.start(0),
                        urlMatcher.end(0)
                    )
                )
            }
            return containedUrls
        }

        fun sendLocalBroadcast(action: String) {
            // Send LocalBroadcast
            val intent = Intent()
            intent.action = action
            LocalBroadcastManager.getInstance(MainApplication.instance).sendBroadcast(intent)
        }

        fun sendLocalBroadcast(action: String, key: String, value: String) {
            // Send LocalBroadcast
            val intent = Intent()
            intent.action = action
            intent.putExtra(key, value)
            LocalBroadcastManager.getInstance(MainApplication.instance).sendBroadcast(intent)
        }
    }
}