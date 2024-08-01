package com.konbini.mdbpayment

import android.app.Application
import android.content.pm.PackageManager
import com.konbini.mdbpayment.utils.Cryptography
import com.konbini.mdbpayment.utils.LogUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    companion object {
        const val TAG = "MainApplication"

        var appStarted = false
        lateinit var instance: MainApplication
        lateinit var cryptography: Cryptography
        var currentVersion: String = "Version: N/A"

        fun shared(): MainApplication {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handleUncaughtException(
                e
            )
        }

        instance = this
        LogUtils.logInfo("Start App")
        cryptography = Cryptography("Konbini63")
        initSetting()
    }

    private fun handleUncaughtException(e: Throwable) {
        LogUtils.logCrash(e)
    }

    private fun initSetting() {
        AppSettings.getAllSetting()
    }

    /**
     * Get current version.
     */
    fun getAppVersion(): String {
        return try {
            val pInfo = this.packageManager.getPackageInfo(this.packageName, 0)
            currentVersion = "Version: " + pInfo.versionName
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }
}