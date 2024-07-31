package com.konbini.mdbpayment.base

import com.konbini.mdbpayment.utils.LogUtils
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.concurrent.TimeUnit

object OkInterceptor {
    private var mClient: OkHttpClient? = null

    /**
     * Don't forget to remove Interceptors (or change Logging Level to NONE)
     * in production! Otherwise people will be able to see your request and response on Log Cat.
     */
    val client: OkHttpClient
        @Throws(NoSuchAlgorithmException::class, KeyManagementException::class)
        get() {
            if (mClient == null) {
                val httpBuilder = OkHttpClient.Builder()
                httpBuilder
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                val interceptor = HttpLoggingInterceptor(HttpLoggingInterceptor.Logger {
                    if (!ignoreLog(log = it))
                        LogUtils.logApi(it)
                })
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                httpBuilder.addInterceptor(interceptor)  /// show all JSON in logCat
                mClient = httpBuilder.protocols(listOf(Protocol.HTTP_1_1))
                    .build()
            }
            return mClient!!
        }

    private fun ignoreLog(log: String): Boolean {
        if (log.contains("Content-Type:")) return true
        if (log.contains("Content-Length:")) return true
        if (log.contains("x-robots-tag:")) return true
        if (log.contains("link:")) return true
        if (log.contains("x-content-type-options:")) return true
        if (log.contains("access-control-expose-headers:")) return true
        if (log.contains("access-control-allow-headers:")) return true
        if (log.contains("allow:")) return true
        if (log.contains("vary:")) return true
        if (log.contains("transfer-encoding:")) return true
        if (log.contains("date:")) return true
        if (log.contains("server:")) return true
        if (log.contains("alt-svc:")) return true
        if (log.contains("connection:")) return true
        if (log.trim().isEmpty()) return true
        return false
    }
}