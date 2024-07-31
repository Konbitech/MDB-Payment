package com.konbini.mdbpayment.data.repository

import com.konbini.mdbpayment.data.remote.fiuu.FiuuRemoteDataSource
import okhttp3.RequestBody
import javax.inject.Inject

class FiuuRepository @Inject constructor(
    private val remoteDataSource: FiuuRemoteDataSource
) {
    suspend fun paymentRequestDirectServer(requestBody: RequestBody) =
        remoteDataSource.paymentRequestDirectServer(requestBody = requestBody)

    suspend fun viewQR(url: String, requestBody: RequestBody) =
        remoteDataSource.viewQR(url = url, requestBody = requestBody)

    suspend fun directStatusRequery(requestBody: RequestBody) =
        remoteDataSource.directStatusRequery(requestBody = requestBody)
}