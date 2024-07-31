package com.konbini.mdbpayment.data.remote.fiuu

import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.data.remote.base.BaseDataSource
import okhttp3.RequestBody
import javax.inject.Inject

class FiuuRemoteDataSource @Inject constructor(
    private val fiuuService: FiuuService
) : BaseDataSource() {
    suspend fun paymentRequestDirectServer(
        requestBody: RequestBody
    ) = getResultWithError {
        val url = AppSettings.Fiuu.PaymentUrl + AppSettings.APIs.PaymentRequestDirectServer
        fiuuService.paymentRequestDirectServer(
            url = url, requestBody = requestBody
        )
    }

    suspend fun viewQR(
        url: String,
        requestBody: RequestBody
    ) = getResultWithError {
        fiuuService.viewQR(
            url = url, requestBody = requestBody
        )
    }

    suspend fun directStatusRequery(
        requestBody: RequestBody
    ) = getResultWithError {
        val url = AppSettings.Fiuu.APIUrl + AppSettings.APIs.DirectStatusRequery
        fiuuService.directStatusRequery(
            url = url, requestBody = requestBody
        )
    }
}