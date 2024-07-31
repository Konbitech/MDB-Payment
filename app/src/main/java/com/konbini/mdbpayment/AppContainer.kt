package com.konbini.mdbpayment

import java.util.Timer

object AppContainer {
    object GlobalVariable {
        var timerReplyBeginSessionJob = Timer()
    }

    object CurrentTransaction {
        var totalPrice = 0.00
    }
}