package com.konbini.mdbpayment.data.remote.fiuu

import com.google.gson.JsonObject
import com.konbini.mdbpayment.data.remote.fiuu.response.DirectStatusRequeryResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface FiuuService {
    @POST
    suspend fun paymentRequestDirectServer (
        @Url url: String,
        @Body requestBody: RequestBody
    ): Response<JsonObject>

    @POST
    suspend fun viewQR(
        @Url url: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun directStatusRequery (
        @Url url: String,
        @Body requestBody: RequestBody
    ): Response<DirectStatusRequeryResponse>
}