package com.konbini.mdbpayment.ui.mainActivity

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.konbini.mdbpayment.AppContainer
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.BuildConfig
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
class MainActivity : BaseActivity(), PaymentModeAdapter.ItemListener {

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
                    val itemPrice = msg.arg1
                    val itemNumber = msg.arg2
                    val amount = itemPrice * 1.00
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

                MSG_MDB_END_SESSION -> {}
                else -> {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**start cashless processor */
        try {
            mProcessor = MdbReaderProcessor(mHandler)
            mProcessor!!.start()
        } catch (ex: Exception) {
            LogUtils.logException(ex)
        }


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

        showIdleMode()
        hidePaymentMode()
        setupRecyclerView()
        gettingFiuuData()

        val scheduleReplyBeginSession = object : TimerTask() {
            override fun run() {
                // VMC Begin
                mProcessor?.let { _mProcessor ->
                    if (_mProcessor.stateMachine == MdbReaderEventMonitorImpl.StateMachine.Enabled) {
                        _mProcessor.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_BEGIN_SESSION)
                    } else {
                        Toast.makeText(this@MainActivity,"mdbReader is not Enable state. ${_mProcessor.stateMachine}",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        AppContainer.GlobalVariable.timerReplyBeginSessionJob.schedule(scheduleReplyBeginSession, 0, 1000)

        lifecycleScope.launch {
            mProcessor?.let { _mProcessor ->
                delay(10000)
                _mProcessor.setReaderEnable()
            }

//            // TODO: Hardcode for test
//            if (BuildConfig.DEBUG) {
//                delay(2000)
//                messageDialogFragment.dismiss()
//                hideIdleMode()
//                showPaymentMode()
//            }
        }
    }

    /**
     * Setup recycler view
     *
     */
    private fun setupRecyclerView() {
        LogUtils.logInfo("Setup RecyclerView")
        initRecyclerViewPayments()
    }

    /**
     * Init recycler view payments
     *
     */
    private fun initRecyclerViewPayments() {
        listPaymentType.clear()
        if (AppSettings.PaymentMode.MasterCardVisa) {
            listPaymentType.add(PaymentModeType.MASTER_CARD_VISA.value)
        }
        if (AppSettings.PaymentMode.EzLink) {
            listPaymentType.add(PaymentModeType.EZ_LINK.value)
        }
        if (AppSettings.PaymentMode.PayNow) {
            listPaymentType.add(PaymentModeType.PAYNOW.value)
        }
        if (AppSettings.PaymentMode.AliPay) {
            listPaymentType.add(PaymentModeType.ALIPAY.value)
        }
        if (AppSettings.PaymentMode.GrabPay) {
            listPaymentType.add(PaymentModeType.GRABPAY.value)
        }
        if (AppSettings.PaymentMode.WeChat) {
            listPaymentType.add(PaymentModeType.WECHAT.value)
        }
        if (AppSettings.PaymentMode.KonbiniWallet) {
            listPaymentType.add(PaymentModeType.KONBI_WALLET.value)
        }

        val coefficient = sqrt(listPaymentType.size.toDouble())
        val spanRow = coefficient.roundToInt()
        val spanCount =
            if (spanRow.toDouble().pow(2) > listPaymentType.size) spanRow else spanRow + 1
        val height = ((resources.displayMetrics.heightPixels - (resources.displayMetrics.heightPixels * 0.20)) / spanRow) - 5

        paymentModeAdapter = PaymentModeAdapter(this, height.toInt())
        val manager =
            GridLayoutManager(this, spanCount, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewPayments.layoutManager = manager
        binding.recyclerViewPayments.adapter = paymentModeAdapter
        paymentModeAdapter.setItems(items = ArrayList(listPaymentType))
    }

    // region ================Event onClicked of Adapter================
    override fun onClickedPaymentMode(payment: String) {
        lastTouch = 0L

        when (payment) {
            PaymentModeType.MASTER_CARD_VISA.value -> {
                handleFiuuVisaMasterCard()
            }
            PaymentModeType.EZ_LINK.value -> {
                showWarningBeingUpgraded()
            }
            PaymentModeType.PAYNOW.value -> {
                handleFiuuPayNow()
            }
            PaymentModeType.ALIPAY.value -> {
                showWarningBeingUpgraded()
            }
            PaymentModeType.GRABPAY.value -> {
                showWarningBeingUpgraded()
            }
            PaymentModeType.WECHAT.value -> {
                showWarningBeingUpgraded()
            }
            PaymentModeType.KONBI_WALLET.value -> {
                showWarningBeingUpgraded()
            }
        }

    }
    // endregion

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
    // endregion

    // region ================Handle Fiuu================
    private fun gettingFiuuData() {
        // getting the data from our
        // intent in our uri.
        val uri: Uri? = intent.data
        // checking if the uri is null or not.
        if (uri != null) {
            lifecycleScope.launch {
                val parameters: MutableList<String> = uri.queryParameterNames.toMutableList()
                LogUtils.logInfo("Uri data: ${Gson().toJson(parameters)}")
                if (parameters.contains("errorCode") && parameters.contains("errorMsg")) {
                    val message = "${uri.getQueryParameter("errorCode")}: ${uri.getQueryParameter("errorMsg")}"
                    messageDialogFragment = MessageDialogFragment(
                        type = Resource.Status.ERROR.name,
                        message = message
                    )
                    messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)
                } else {
                    val status = uri.getQueryParameter("status")
                    if (status == "00") {
                        val orderId = uri.getQueryParameter("orderid").toString()
                        handleFiuuPaymentSuccess(orderId)
                    }
                }
            }
        }
    }

    private fun handleFiuuPaymentSuccess(orderId: String) {
        lifecycleScope.launch {
            messageDialogFragment = MessageDialogFragment(
                type = Resource.Status.SUCCESS.name,
                message = "Order $orderId Payment Success"
            )
            messageDialogFragment.show(supportFragmentManager, MessageDialogFragment.TAG)
            delay(1000)
            messageDialogFragment.setIcon(type = Resource.Status.LOADING.name)
            messageDialogFragment.setMessage(message = "REPLY VEND APPROVED")
            // REPLY VEND APPROVED
            mProcessor!!.setPollReply(MdbReaderEventMonitorImpl.PollReply.REPLY_VEND_APPROVED)
            delay(1000)
            messageDialogFragment.dialog!!.dismiss()

            hidePaymentMode()
            showIdleMode()
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
                messageDialogFragment.dialog?.let {
                    it.dismiss()
                }
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
    }
    // endregion
}