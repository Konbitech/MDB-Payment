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
}