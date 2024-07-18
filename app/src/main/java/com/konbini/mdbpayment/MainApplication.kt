package com.konbini.mdbpayment

import android.app.Application
import android.content.pm.PackageManager
import com.konbini.mdbpayment.utils.LogUtils
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    companion object {
        const val TAG = "MainApplication"

        lateinit var instance: MainApplication
        var currentVersion: String = "Version: N/A"

        fun shared(): MainApplication {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        LogUtils.logInfo("Start App")
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handleUncaughtException(
                e
            )
        }
    }

    private fun handleUncaughtException(e: Throwable) {
        LogUtils.logCrash(e)
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