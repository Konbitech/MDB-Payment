package com.konbini.mdbpayment.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.konbini.mdbpayment.MainApplication
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    private const val TAG = "LOGGER"

    fun d(message: String) {
        val ste = Throwable().stackTrace
        val text = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()]"
        Log.d(TAG, text + message)
    }

    fun i(message: String) {
        val ste = Throwable().stackTrace
        val text = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()]"
        Log.i(TAG, text + message)
    }

    fun e(message: String) {
        val ste = Throwable().stackTrace
        val text =
            "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] !!!WARNING "
        Log.e(TAG, text + message)
    }

    fun logInfo(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message)
    }

    fun logException(e: Exception) {
        val ste = Throwable().stackTrace
        val stacktrace = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString().trim()

        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + stacktrace
        e.printStackTrace()
        writeLog(message, "Exception")
    }

    fun logError(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, "Error")
    }

    fun logTerminal(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, "Terminal")
    }

    fun logMagicPlate(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, "MagicPlate")
    }

    fun logCrash(r: Throwable) {
        val ste = r.stackTrace
        val stacktrace = StringWriter().also { r.printStackTrace(PrintWriter(it)) }.toString().trim()

        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + stacktrace
        Log.e("Crash", message)

        writeLog(message, "Crash")
    }

    fun logCloudSync(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, "CloudSync")
    }

    fun logOffline(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, "OfflineSync")
    }

    fun logStoreLocalData(log: String) {
        val ste = Throwable().stackTrace
        val message = "[" + ste[1].fileName + ":" + ste[1].lineNumber + ":" + ste[1].methodName + "()] " + log
        writeLog(message, "StoreLocalData")
    }

    fun logApi(log: String) {
        writeLog(log, "Api")
    }

    fun logMqtt(log: String) {
        writeLog(log, "MQTT")
    }

    fun logAttachedDetached(log: String) {
        writeLog(log, "AttachedDetached")
    }

    fun logLogcat(context: Context) {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendarTime = Calendar.getInstance().time
        val currentDate = formatter.format(calendarTime)

        val filename = "${currentDate}_Logcat.txt"
        val outputFile = File(context.externalCacheDir, filename)
        Runtime.getRuntime().exec("logcat -f ${outputFile.absolutePath} *:W")

    }

    @SuppressLint("SimpleDateFormat")
    private fun writeLog(text: String?, fileName: String = "Info") {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        val formatterDateFolder = SimpleDateFormat("yyyy_MM_dd")
        val formatterTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val calendarTime = Calendar.getInstance().time
        val currentDate = formatter.format(calendarTime)

        val downloadFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        // Check MDB folder
        val mdbFolder = File(downloadFolderPath, "MDB")
        if (!mdbFolder.exists())
            try {
                mdbFolder.mkdir()
            } catch (ex: IOException) {
                Log.d("ERROR", ex.message.toString())
            }

        // Check MagicPlate/Logs folder
        val logsFolder = File(mdbFolder, "Logs")
        if (!logsFolder.exists())
            try {
                logsFolder.mkdir()
            } catch (ex: IOException) {
                Log.d("ERROR", ex.message.toString())
            }

        // Check Date folder
        val dateFolder = File(logsFolder, formatterDateFolder.format(calendarTime))
        if (!dateFolder.exists())
            try {
                dateFolder.mkdir()
            } catch (ex: IOException) {
                Log.d("ERROR", ex.message.toString())
            }

        var fileLog = File(dateFolder, "${currentDate}_${fileName}.txt")
        try {
            if (!fileLog.exists()) {
                fileLog.createNewFile()
                fileLog.canRead()
                fileLog.canWrite()
            } else {
                if (!fileLog.canRead()) {
                    fileLog = File(dateFolder, "${currentDate}_${fileName}_${MainApplication.instance.getAppVersion()}.txt")
                    fileLog.canRead()
                    fileLog.canWrite()
                }
            }

            val buf = BufferedWriter(FileWriter(fileLog, true))
            val msg = "${formatterTime.format(calendarTime)} : $text"
            buf.append(msg)
            buf.newLine()
            buf.close()
            Log.d("Logger-$fileName", msg)
        } catch (ex: IOException) {
            Log.d("ERROR", ex.message.toString())
        }
    }
}