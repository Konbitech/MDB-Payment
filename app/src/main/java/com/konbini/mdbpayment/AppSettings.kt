package com.konbini.mdbpayment

import com.konbini.mdbpayment.utils.PrefUtil

object AppSettings {
    private const val nameSpace = "com.konbini.mdbpayment"

    fun getAllSetting() {
        getChildClasses(this.javaClass.declaredClasses)
    }

    private fun getChildClasses(classes: Array<Class<*>?>?) {
        if (classes != null) {
            if (classes.isNotEmpty()) {
                classes.forEach { cl ->
                    val childCl = cl?.declaredClasses
                    if (cl?.declaredFields?.isNotEmpty() == true) {
                        val className = cl.name.replace("$", ".")

                        cl.declaredFields.forEach { field ->
                            val fieldName = field.name
                            val instance = cl.declaredFields[0]

                            if (!fieldName.endsWith("INSTANCE")) {
                                field.isAccessible = true
                                var value: Any? = null
                                val spKey = "$className.$fieldName".replace("$nameSpace.", "")
                                //val spKey = fieldName.toString()
                                val fieldType = field.type.name

                                // Get SP value, if not found return default value of property
                                when (fieldType) {
                                    "java.lang.String" -> {
                                        val stringValue = field.get(instance)?.toString()
                                        value = PrefUtil.getString(spKey, stringValue ?: "")
                                        field.set(instance, value)
                                    }
                                    "int" -> {
                                        val intValue = field.getInt(instance)
                                        val spValue = PrefUtil.getInt(spKey, intValue)
                                        value = spValue
                                        field.setInt(instance, spValue)
                                    }
                                    "long" -> {
                                        val longValue = field.getLong(instance)
                                        val spValue = PrefUtil.getLong(spKey, longValue)
                                        value = spValue
                                        field.setLong(instance, spValue)
                                    }
                                    "boolean" -> {
                                        val boolValue = field.getBoolean(instance)
                                        val spValue = PrefUtil.getBoolean(spKey, boolValue)
                                        value = spValue
                                        field.setBoolean(instance, spValue)
                                    }
                                }
                                //Log.d("TEST", "$spKey ($fieldType) = $value")
                            }
                        }
                    }
                    getChildClasses(childCl)
                }
            }
        }
    }

    object PaymentMode {
        var MasterCardVisa = true
        var pathImageMasterCardVisa = ""

        var EzLink = true
        var pathImageEzLink = ""

        var PayNow = true
        var pathImagePayNow = ""

        var AliPay = true
        var pathImageAliPay = ""

        var GrabPay = true
        var pathImageGrabPay = ""

        var WeChat = true
        var pathImageWeChat = ""

        var KonbiniWallet = true
        var pathImageKonbiniWallet = ""

        var Cash = true
        var pathImageCash = ""
    }

    object Fiuu {
        var PaymentUrl = "https://pay.merchant.razer.com"
        var APIUrl = "https://api.merchant.razer.com"
        var MerchantID = "konbini_Dev"
        var VerifyKey = "163fa3774f82328b566d9e2f752435c5"
        var PrivateKey = "afc2daf45d590bdf9c4f17e312dcbb45"
        var ReferenceNo = "18570"
        var TxnType = "SALS"
        var TxnChannel = "PAYNOW"
        var TxnCurrency = "SGD"
        var CustName = "RMS+Demo"
        var Custemail = "demo@RMS.com"
        var CustContact = "55218438"
        var CustDesc = "testing+by+RMS"
    }

    object Options {
        object Payment {
            var Timeout = 60L
        }
    }

    object APIs {
        // FIUU
        var PaymentRequestDirectServer = "/RMS/API/Direct/1.4.0/index.php"
        var ViewQR = "/RMS/PayNow/view_qr.php"
        var DirectStatusRequery = "/RMS/API/gate-query/index.php"
    }
}