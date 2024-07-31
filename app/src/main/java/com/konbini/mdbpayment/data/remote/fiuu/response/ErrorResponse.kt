package com.konbini.mdbpayment.data.remote.fiuu.response

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("status"     ) var status    : Boolean? = null,
    @SerializedName("error_code" ) var errorCode : String?  = null,
    @SerializedName("error_desc" ) var errorDesc : String?  = null
)
