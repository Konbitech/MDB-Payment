package com.konbini.mdbpayment.ui.mainActivity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
//import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.konbini.mdbpayment.AppContainer
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.BroadcastKey
import com.konbini.mdbpayment.R
import com.konbini.mdbpayment.data.enum.FiuuAppendixA
import com.konbini.mdbpayment.data.enum.FiuuAppendixB
import com.konbini.mdbpayment.data.enum.PaymentModeType
import com.konbini.mdbpayment.data.remote.fiuu.response.ErrorResponse
import com.konbini.mdbpayment.data.remote.fiuu.response.PaymentRequestDirectServerResponse
import com.konbini.mdbpayment.databinding.ActivityMainBinding
import com.konbini.mdbpayment.hardware.MdbReaderEventMonitorImpl
import com.konbini.mdbpayment.hardware.MdbReaderProcessor
import com.konbini.mdbpayment.ui.adapters.PaymentModeAdapter
import com.konbini.mdbpayment.ui.baseActivity.BaseActivity
import com.konbini.mdbpayment.ui.dialog.MessageDialogFragment
import com.konbini.mdbpayment.ui.dialog.QRDialogFragment
import com.konbini.mdbpayment.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.util.ArrayList
import java.util.TimerTask
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@AndroidEntryPoint
class MainActivity : BaseActivity() {//, PaymentModeAdapter.ItemListener {

    companion object {
        const val TAG = "MainActivity"
        const val TIMEOUT_SELECT_PAYMENT = 60 // Seconds
        const val MSG_MDB_INITIAL_COMPLETE = 1
        const val MSG_MDB_BEGIN_SESSION = MSG_MDB_INITIAL_COMPLETE + 1
        const val MSG_MDB_VEND = MSG_MDB_BEGIN_SESSION + 1
        const val MSG_MDB_END_SESSION = MSG_MDB_VEND + 1
    }

    private var lastTouch = System.currentTimeMillis()
    private var mProcessor: MdbReaderProcessor? = null
    private var listPaymentType: MutableList<String> = mutableListOf()

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var paymentModeAdapter: PaymentModeAdapter
    private lateinit var messageDialogFragment: MessageDialogFragment

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BroadcastKey.FIUU_QR_RESULT -> {
                    val result: State = Gson().fromJson(intent.getStringExtra(BroadcastKey.FIUU_QR_RESULT).toString(), State::class.java)
                    if (result.status == Resource.Status.SUCCESS) {
                        handleFiuuPaymentSuccess()
                    } else {
                        showIdleMode()
                        hidePaymentMode()
                        messageDialogFragment = MessageDialogFragment(
                            type = Resource.Status.ERROR.name,
                            message = result.message
                        )
                        messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)
                    }
                }
            }
        }
    }

    private val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val bdl = msg.data
            when (msg.what) {
                MSG_MDB_INITIAL_COMPLETE -> {
                    // val strInfo = bdl.getString("msg")
                    messageDialogFragment.dialog?.let { dialog ->
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }
                    // VMC Begin
                    if (mProcessor!!.stateMachine == MdbReaderEventMonitorImpl.StateMachine.Enabled) {
                        mProcessor!!.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_BEGIN_SESSION)
                    }
                }

                MSG_MDB_BEGIN_SESSION -> {}
                MSG_MDB_VEND -> {
                    val itemPrice = msg.arg1 // Cent
                    val itemNumber = msg.arg2
                    LogUtils.logInfo("itemPrice: $itemPrice - itemNumber: $itemNumber")
                    val amount = (itemPrice / 100.00) * 1.00
                    AppContainer.CurrentTransaction.totalPrice = amount

                    hideIdleMode()
                    showPaymentMode()
                    setAmountValue(number = itemNumber, amount = amount)
//                    val itemPrice = msg.arg1
//                    val itemNumber = msg.arg2
//                    val vend_msg = """
//                    Item Number: ${Integer.toString(itemNumber)}
//                    Item Price: ${Integer.toString(itemPrice)}
//                    Allow the selected goods to be dispensed ?
//                    """.trimIndent()
//                    val builder = AlertDialog.Builder(mContext)
//                    builder.setTitle("vend request")
//                    builder.setCancelable(false)
//                    builder.setMessage(vend_msg)
//                    builder.setPositiveButton(
//                        "Approve"
//                    ) { dialog, which ->
//                        mProcessor.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_VEND_APPROVED)
//                        mResultTextView.setText("Vend Approved.")
//                    }
//                    builder.setNegativeButton(
//                        "Denied"
//                    ) { dialog, which ->
//                        mProcessor.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_VEND_DENIED)
//                        mResultTextView.setText("Vend Denied.")
//                    }
//                    builder.setNeutralButton(
//                        "Cancel"
//                    ) { dialog, which ->
//                        mProcessor.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_SESSION_CANCEL_REQUEST)
//                        mResultTextView.setText("Session Cancel.")
//                    }
//                    val alertDialog = builder.create()
//                    alertDialog.show()
                }

                MSG_MDB_END_SESSION -> {
                    LogUtils.logInfo("MSG_MDB_END_SESSION")
                    reStartActivity()
                }
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!this::messageDialogFragment.isInitialized) {
            messageDialogFragment = MessageDialogFragment(
                type = Resource.Status.LOADING.name,
                message = String.format(
                    getString(R.string.message_waiting_initial_s),
                    getString(R.string.title_VMC)
                )
            )
            messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)
        }

        /**start cashless processor */
        try {
            mProcessor = null
            mProcessor = MdbReaderProcessor(mHandler)
            mProcessor!!.start()
        } catch (ex: Exception) {
            LogUtils.logException(ex)
        }

        showIdleMode()
        hidePaymentMode()
        gettingFiuuData()
        setupActions()
    }

    override fun onStart() {
        super.onStart()
        val filterIntent = IntentFilter()
        filterIntent.addAction(BroadcastKey.FIUU_QR_RESULT)
        LocalBroadcastManager.getInstance(applicationContext)
            .registerReceiver(broadcastReceiver, IntentFilter(filterIntent))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(applicationContext).unregisterReceiver(broadcastReceiver)
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun setupActions() {
        binding.layoutPaymentMode.masterVisaMode.setSafeOnClickListener {
            handleFiuuVisaMasterCard()
        }

        binding.layoutPaymentMode.ezLinkMode.setSafeOnClickListener {
            showWarningBeingUpgraded()
        }

        binding.layoutPaymentMode.payNowMode.setSafeOnClickListener {
            handleFiuuPayNow()
        }

        binding.layoutPaymentMode.alipayMode.setSafeOnClickListener {
            handleFiuuAliPay()
        }

        binding.layoutPaymentMode.grabPayMode.setSafeOnClickListener {
            handleFiuuGrabPay()
        }

        binding.layoutPaymentMode.weChatMode.setSafeOnClickListener {
            handleFiuuWechat()
        }
    }

//    // region ================Event onClicked of Adapter================
//    override fun onClickedPaymentMode(payment: String) {
//        lastTouch = 0L
//        LogUtils.logInfo("Selected $payment")
//
//        when (payment) {
//            PaymentModeType.MASTER_CARD_VISA.value -> {
//                handleFiuuVisaMasterCard()
//            }
//            PaymentModeType.EZ_LINK.value -> {
//                showWarningBeingUpgraded()
//            }
//            PaymentModeType.PAYNOW.value -> {
//                handleFiuuPayNow()
//            }
//            PaymentModeType.ALIPAY.value -> {
//                showWarningBeingUpgraded()
//            }
//            PaymentModeType.GRABPAY.value -> {
//                showWarningBeingUpgraded()
//            }
//            PaymentModeType.WECHAT.value -> {
//                showWarningBeingUpgraded()
//            }
//            PaymentModeType.KONBI_WALLET.value -> {
//                showWarningBeingUpgraded()
//            }
//        }
//
//    }
//    // endregion

    // region ================Handle UI================
    private fun setAmountValue(number: Int, amount: Double) {
        binding.messageAmount.text = String.format(
            getString(R.string.message_amount_s_s),
            CommonUtil.formatCurrency(value = amount),
            String.format("%03d", number)
        )
    }

    private fun showIdleMode() {
        binding.idleMode.visibility = View.VISIBLE
    }

    private fun hideIdleMode() {
        binding.idleMode.visibility = View.GONE
    }

    private fun showPaymentMode() {
        lastTouch = System.currentTimeMillis()
        binding.paymentMode.visibility = View.VISIBLE
        startCheckTimeout()
    }

    private fun hidePaymentMode() {
        binding.paymentMode.visibility = View.GONE
    }

    private fun startCheckTimeout() {
        lifecycleScope.launch {
            while (lastTouch > 0L) {
                if ((System.currentTimeMillis() - lastTouch) / 1000 > TIMEOUT_SELECT_PAYMENT) {
                    showIdleMode()
                    hidePaymentMode()
                    lastTouch = 0L
                }
                Log.e(TAG, "Checking Timeout Select Payment")
                delay(1000)
            }
        }
    }

    private fun showWarningBeingUpgraded() {
        messageDialogFragment = MessageDialogFragment(
            type = Resource.Status.ERROR.name,
            message = getString(R.string.message_function_is_being_upgraded)
        )
        messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)
    }

    private fun reStartActivity() {
        LogUtils.logInfo("Restart Activity")
        val intent = intent
        intent.data = null
        finish()
        startActivity(intent)
    }
    // endregion

    // region ================Handle Fiuu================
    private fun gettingFiuuData() {
        lifecycleScope.launch {
            // getting the data from our
            // intent in our uri.
            val uri: Uri? = intent.data
            // checking if the uri is null or not.
            if (uri != null) {
                LogUtils.logInfo("URI: $uri")
                messageDialogFragment.dialog?.let { _dialog ->
                    _dialog.dismiss()
                }
                val parameters: MutableList<String> = uri.queryParameterNames.toMutableList()
                if (parameters.contains("errorCode") && parameters.contains("errorMsg")) {
                    val message = "${uri.getQueryParameter("errorCode")}: ${uri.getQueryParameter("errorMsg")}"
                    LogUtils.logInfo(message)
                    messageDialogFragment = MessageDialogFragment(
                        type = Resource.Status.ERROR.name,
                        message = message
                    )
                    messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)

                    delay(3000)
                    messageDialogFragment.dialog?.let { _dialog ->
                        _dialog.dismiss()
                    }
                    reStartActivity()
                } else {
                    val status = uri.getQueryParameter("status")
                    if (status == "00") {
//                        val orderId = uri.getQueryParameter("orderid").toString()
                        handleFiuuPaymentSuccess()
                    }
                }
            } else {
//                mProcessor?.let { _mProcessor ->
//                    delay(2000)
//                    _mProcessor.setReaderEnable()
//                    if (_mProcessor.stateMachine == MdbReaderEventMonitorImpl.StateMachine.Enabled) {
//                        _mProcessor.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_BEGIN_SESSION)
//                    }
//                }
                delay(2000)
                messageDialogFragment.dialog?.let { _dialog ->
                    _dialog.dismiss()
                }
            }
        }
    }

    private fun handleFiuuPaymentSuccess() {
        lifecycleScope.launch {
            LogUtils.logInfo("Handle payment success")
            LogUtils.logInfo("Show dialog payment success")
            messageDialogFragment = MessageDialogFragment(
                type = Resource.Status.SUCCESS.name,
                message = "Payment Success"
            )
            messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)
            delay(1000)
            messageDialogFragment.setIcon(type = Resource.Status.LOADING.name)
            messageDialogFragment.setMessage(message = "REPLY VEND APPROVED")
            // REPLY VEND APPROVED
            LogUtils.logInfo("Set poll REPLY_VEND_APPROVED")
            mProcessor!!.setStateMachineIsVend()
            mProcessor!!.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_VEND_APPROVED)
            delay(1000)
            messageDialogFragment.dialog!!.dismiss()

            showIdleMode()
            hidePaymentMode()

            delay(3000)
            reStartActivity()
        }
    }

    private fun handleFiuuVisaMasterCard() {
        FiuuUtil.callFiuuApp(
            activity = this,
            opType = FiuuAppendixA.SALE.value,
            channel = FiuuAppendixB.CARD.value
        )
    }

    private fun handleFiuuPayNow() {
        if (AppSettings.Fiuu.isAPI) {
            messageDialogFragment = MessageDialogFragment(
                type = Resource.Status.LOADING.name,
                message = getString(R.string.message_loading)
            )
            messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)

            AppContainer.CurrentTransaction.totalPrice = 1.00
            val amount = "%.2f".format(AppContainer.CurrentTransaction.totalPrice).toDouble()
            LogUtils.logInfo("Amount: $amount")
            lifecycleScope.launch {
                val payment = viewModel.paymentRequestDirectServer(amount = amount)
                if (payment.status == Resource.Status.ERROR) {
                    val errorDetail = Gson().fromJson(payment.message, ErrorResponse::class.java)
                    val message = "${errorDetail.errorCode}: ${errorDetail.errorDesc}"

                    // Show Error Message
                    messageDialogFragment.setIcon(type = Resource.Status.ERROR.name)
                    messageDialogFragment.setMessage(message = message)
                    return@launch
                }

                val jsonObject = payment.data as JsonObject
                if (jsonObject.has("error_code")) {
                    val errorDetail = Gson().fromJson(payment.data.toString(), ErrorResponse::class.java)
                    val message = "${errorDetail.errorCode}: ${errorDetail.errorDesc}"

                    // Show Error Message
                    messageDialogFragment.setIcon(type = Resource.Status.ERROR.name)
                    messageDialogFragment.setMessage(message = message)
                    return@launch
                } else {
                    // Show QR Code
                    val paymentData = Gson().fromJson(payment.data.toString(), PaymentRequestDirectServerResponse::class.java)
                    paymentData.TxnData?.let { txnData ->
                        txnData.RequestData?.let { requestData ->
                            val viewQr = viewModel.viewQr(url = txnData.RequestURL.toString(), requestData = requestData)
                            if (payment.status == Resource.Status.ERROR) {

                            }
                            val viewQrData = (viewQr.data as ResponseBody).string()
                            val links = CommonUtil.extractUrls(viewQrData)
                            val find = links.find { it.contains("view_qr.php") }
                            find?.let {
                                messageDialogFragment.dialog?.let { _dialog ->
                                    _dialog.dismiss()
                                }

                                val qrDialogFragment = QRDialogFragment(
                                    url = it,
                                    txnAmount = amount,
                                    txID = requestData.orderid.toString()
                                )
                                qrDialogFragment.show(supportFragmentManager, QRDialogFragment.TAG)
                            }
                        }
                    }
                }
            }
        } else {
            FiuuUtil.callFiuuApp(
                activity = this,
                opType = FiuuAppendixA.SALE.value,
                channel = FiuuAppendixB.PAYNOW.value
            )
        }
    }

    private fun handleFiuuAliPay() {
        FiuuUtil.callFiuuApp(
            activity = this,
            opType = FiuuAppendixA.SALE.value,
            channel = FiuuAppendixB.ALIPAY_OFFLINE.value
        )
    }

    private fun handleFiuuGrabPay() {
        FiuuUtil.callFiuuApp(
            activity = this,
            opType = FiuuAppendixA.SALE.value,
            channel = FiuuAppendixB.GRABPAY_OFFLINE.value
        )
    }

    private fun handleFiuuWechat() {
        FiuuUtil.callFiuuApp(
            activity = this,
            opType = FiuuAppendixA.SALE.value,
            channel = FiuuAppendixB.WECHATPAYMY_OFFLINE.value
        )
    }
    // endregion
}