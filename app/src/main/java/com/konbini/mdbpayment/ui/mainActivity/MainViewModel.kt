package com.konbini.mdbpayment.ui.mainActivity

import androidx.lifecycle.ViewModel
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.data.remote.fiuu.response.RequestData
import com.konbini.mdbpayment.data.repository.FiuuRepository
import com.konbini.mdbpayment.utils.FiuuUtil
import com.konbini.mdbpayment.utils.LogUtils
import com.konbini.mdbpayment.utils.Resource
import com.konbini.mdbpayment.utils.State
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fiuuRepository: FiuuRepository
) : ViewModel() {
    companion object {
        const val TAG = "MainViewModel"
    }

    // region ================FIUU================
    suspend fun paymentRequestDirectServer(amount: Double): State {
        val result = State()
        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("MerchantID", AppSettings.Fiuu.MerchantID)
            .addFormDataPart("ReferenceNo", AppSettings.Fiuu.ReferenceNo)
            .addFormDataPart("TxnType", AppSettings.Fiuu.TxnType)
            .addFormDataPart("TxnChannel", AppSettings.Fiuu.TxnChannel)
            .addFormDataPart("TxnCurrency", AppSettings.Fiuu.TxnCurrency)
            .addFormDataPart("TxnAmount", amount.toString())
            .addFormDataPart("CustName", AppSettings.Fiuu.CustName)
            .addFormDataPart("Custemail", AppSettings.Fiuu.Custemail)
            .addFormDataPart("CustContact", AppSettings.Fiuu.CustContact)
            .addFormDataPart("CustDesc", AppSettings.Fiuu.CustDesc)
            .addFormDataPart(
                "Signature",
                FiuuUtil.generateSignature(
                    txnAmount = amount.toString(),
                    referenceNo = AppSettings.Fiuu.ReferenceNo
                )
            )
            .build()
        val payment = fiuuRepository.paymentRequestDirectServer(
            requestBody = requestBody
        )
        if (payment.status == Resource.Status.SUCCESS) {
            result.status = Resource.Status.SUCCESS
            result.message = payment.message.toString()
            result.data = payment.data
        } else {
            result.status = Resource.Status.ERROR
            result.message = payment.message.toString()
        }
        return result
    }

    suspend fun viewQr(url: String, requestData: RequestData): State {
        val result = State()
        try {
            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("keys", requestData.keys.toString())
                .addFormDataPart("orderid", requestData.orderid.toString())
                .addFormDataPart("amount", requestData.amount.toString())
                .addFormDataPart("merchantID", requestData.merchantID.toString())
                .addFormDataPart("UEN", requestData.UEN.toString())
                .addFormDataPart("payIndicator", requestData.payIndicator.toString())
                .addFormDataPart("merchantAccInfo", requestData.merchantAccInfo.toString())
                .addFormDataPart(
                    "globalUniqueIdentifier",
                    requestData.globalUniqueIdentifier.toString()
                )
                .addFormDataPart("proxyType", requestData.proxyType.toString())
                .addFormDataPart("proxyValue", requestData.proxyValue.toString())
                .addFormDataPart("amountIndicator", requestData.amountIndicator.toString())
                .addFormDataPart("qrDateTimeXpry", requestData.qrDateTimeXpry.toString())
                .addFormDataPart("merchantCatCode", requestData.merchantCatCode.toString())
                .addFormDataPart("txnCurrency", requestData.txnCurrency.toString())
                .addFormDataPart("txnAmount", requestData.txnAmount.toString())
                .addFormDataPart("countryCode", requestData.countryCode.toString())
                .addFormDataPart("merchantName", requestData.merchantName.toString())
                .addFormDataPart("merchantCity", requestData.merchantCity.toString())
                .addFormDataPart("refNumber", requestData.refNumber.toString())
                .addFormDataPart("checkSum", requestData.checkSum.toString())
                .build()

            val viewQr = fiuuRepository.viewQR(
                url = url,
                requestBody = requestBody
            )
            if (viewQr.status == Resource.Status.SUCCESS) {
                result.status = Resource.Status.SUCCESS
                result.message = viewQr.message.toString()
                result.data = viewQr.data
            } else {
                result.status = Resource.Status.ERROR
                result.message = viewQr.message.toString()
            }
            return result
        } catch (ex: Exception) {
            LogUtils.logException(ex)
            result.status = Resource.Status.ERROR
            result.message = ex.message.toString()
            return result
        }
    }

    suspend fun pollingDirectStatusRequery(txnAmount: Double, txID: String): State {
        val result = State()
        try {
            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("amount",txnAmount.toString())
                .addFormDataPart("txID",txID)
                .addFormDataPart("domain",AppSettings.Fiuu.MerchantID)
                .addFormDataPart("skey",FiuuUtil.generateSkey(
                    txnAmount = txnAmount.toString(),
                    txID = txID
                ))
                .addFormDataPart("type","2")
                .build()
            val status = fiuuRepository.directStatusRequery(
                requestBody = requestBody
            )
            if (status.status == Resource.Status.SUCCESS) {
                result.status = Resource.Status.SUCCESS
                result.message = status.message.toString()
                result.data = status.data
            } else {
                result.status = Resource.Status.ERROR
                result.message = status.message.toString()
            }
            return result
        } catch (ex: Exception) {
            LogUtils.logException(ex)
            result.status = Resource.Status.ERROR
            result.message = ex.message.toString()
            return result
        }
    }
    // endregion
}