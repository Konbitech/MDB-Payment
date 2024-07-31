package com.konbini.mdbpayment.data.enum

enum class PaymentModeType(val value: String) {
    MASTER_CARD_VISA("MASTER_CARD_VISA"),
    EZ_LINK("EZ_LINK"),
    PAYNOW("PAYNOW"),
    ALIPAY("ALIPAY"),
    GRABPAY("GRABPAY"),
    WECHAT("WECHAT"),
    KONBI_WALLET("KONBI_WALLET")
}