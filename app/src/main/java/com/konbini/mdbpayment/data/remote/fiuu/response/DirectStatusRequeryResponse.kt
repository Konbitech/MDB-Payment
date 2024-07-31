package com.konbini.mdbpayment.data.remote.fiuu.response

import com.google.gson.annotations.SerializedName

data class DirectStatusRequeryResponse(
    @SerializedName("StatCode" ) var StatCode : String? = null,
    @SerializedName("StatName" ) var StatName : String? = null,
    @SerializedName("TranID"   ) var TranID   : String? = null,
    @SerializedName("Amount"   ) var Amount   : String? = null,
    @SerializedName("Domain"   ) var Domain   : String? = null,
    @SerializedName("Channel"  ) var Channel  : String? = null,
    @SerializedName("VrfKey"   ) var VrfKey   : String? = null,
    @SerializedName("Currency" ) var Currency : String? = null
)
