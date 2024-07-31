package com.konbini.mdbpayment.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.konbini.mdbpayment.AppContainer
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.data.enum.FiuuAppendixB
import com.konbini.mdbpayment.data.enum.FiuuAppendixC
import java.security.MessageDigest

object FiuuUtil {
    /**
     * Call fiuu app
     *
     * @param activity
     * @param opType
     * @param channel
     * @param payType
     */
    fun callFiuuApp(activity: Activity, opType: String, channel: String, payType: Int = FiuuAppendixC.GENERATE_QR.value) {
        val currency = if (CommonUtil.currency.currencyCode == "USD") "SGD" else CommonUtil.currency.currencyCode
        val amount = "%.2f".format(AppContainer.CurrentTransaction.totalPrice)
        LogUtils.logInfo("Amount: $amount")
        val merchantUrlScheme = "mdbpayment"
        val merchantHost = "konbitech.com"
        val url = "razervt://merchant.razer.com?" +
                "merchantUrlScheme=$merchantUrlScheme" +
                "&merchantHost=$merchantHost" +
                "&opType=$opType" +
                "&currency=$currency" +
                "&amount=$amount" +
                "&orderId=${CommonUtil.formatDateTime("YYMMddHHmmss", System.currentTimeMillis())}" +
                "&channel=$channel" +
                if (channel == FiuuAppendixB.CARD.value)
                    ""
                else
                    "&payType=$payType"
        LogUtils.logInfo("URL: $url")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
    }

    /**
     * Generate signature
     * md5( amount & merchantId & referenceNo & verifyKey )
     *
     * @param txnAmount
     * @param referenceNo
     * @return
     */
    fun generateSignature(txnAmount: String, referenceNo: String): String {
        val merchantId = AppSettings.Fiuu.MerchantID
        val verifyKey = AppSettings.Fiuu.VerifyKey

        val input = "$txnAmount$merchantId$referenceNo$verifyKey"
        //MD5 encryption of STR
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        // Convert the hash bytes to a hex string
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate Skey
     * md5( txID & merchantId & verify_key & amount )
     *
     * @param txnAmount
     * @param txID
     * @return
     */
    fun generateSkey(txnAmount: String, txID: String): String {
        val merchantId = AppSettings.Fiuu.MerchantID
        val verifyKey = AppSettings.Fiuu.VerifyKey

        val input = "$txID$merchantId$verifyKey$txnAmount"
        //MD5 encryption of STR
        val md = MessageDigest.getInstance("MD5")
        val hashBytes = md.digest(input.toByteArray())
        // Convert the hash bytes to a hex string
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}