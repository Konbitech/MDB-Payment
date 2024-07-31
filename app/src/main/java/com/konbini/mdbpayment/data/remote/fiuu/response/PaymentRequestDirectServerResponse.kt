package com.konbini.mdbpayment.data.remote.fiuu.response

import com.google.gson.annotations.SerializedName

data class PaymentRequestDirectServerResponse(
    @SerializedName("Keys"        ) var Keys        : String?  = null,
    @SerializedName("MerchantID"  ) var MerchantID  : String?  = null,
    @SerializedName("ReferenceNo" ) var ReferenceNo : String?  = null,
    @SerializedName("TxnID"       ) var TxnID       : String?  = null,
    @SerializedName("TxnCurrency" ) var TxnCurrency : String?  = null,
    @SerializedName("TxnAmount"   ) var TxnAmount   : String?  = null,
    @SerializedName("TxnChannel"  ) var TxnChannel  : String?  = null,
    @SerializedName("TxnData"     ) var TxnData     : TxnData? = TxnData()
)

data class TxnData (
    @SerializedName("RequestURL"    ) var RequestURL    : String?      = null,
    @SerializedName("RequestMethod" ) var RequestMethod : String?      = null,
    @SerializedName("RequestType"   ) var RequestType   : String?      = null,
    @SerializedName("RequestData"   ) var RequestData   : RequestData? = RequestData()
)

data class RequestData (
    @SerializedName("keys"                   ) var keys                   : String? = null,
    @SerializedName("orderid"                ) var orderid                : String? = null,
    @SerializedName("amount"                 ) var amount                 : String? = null,
    @SerializedName("merchantID"             ) var merchantID             : String? = null,
    @SerializedName("UEN"                    ) var UEN                    : String? = null,
    @SerializedName("payIndicator"           ) var payIndicator           : String? = null,
    @SerializedName("merchantAccInfo"        ) var merchantAccInfo        : String? = null,
    @SerializedName("globalUniqueIdentifier" ) var globalUniqueIdentifier : String? = null,
    @SerializedName("proxyType"              ) var proxyType              : String? = null,
    @SerializedName("proxyValue"             ) var proxyValue             : String? = null,
    @SerializedName("amountIndicator"        ) var amountIndicator        : String? = null,
    @SerializedName("qrDateTimeXpry"         ) var qrDateTimeXpry         : String? = null,
    @SerializedName("merchantCatCode"        ) var merchantCatCode        : String? = null,
    @SerializedName("txnCurrency"            ) var txnCurrency            : String? = null,
    @SerializedName("txnAmount"              ) var txnAmount              : String? = null,
    @SerializedName("countryCode"            ) var countryCode            : String? = null,
    @SerializedName("merchantName"           ) var merchantName           : String? = null,
    @SerializedName("merchantCity"           ) var merchantCity           : String? = null,
    @SerializedName("refNumber"              ) var refNumber              : String? = null,
    @SerializedName("checkSum"               ) var checkSum               : String? = null
)