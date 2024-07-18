package com.konbini.mdbpayment.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.R
import com.konbini.mdbpayment.data.enum.PaymentModeType
import com.konbini.mdbpayment.databinding.ActivityMainBinding
import com.konbini.mdbpayment.hardware.MdbReaderProcessor
import com.konbini.mdbpayment.ui.adapters.PaymentModeAdapter
import com.konbini.mdbpayment.ui.dialog.MessageDialogFragment
import com.konbini.mdbpayment.utils.CommonUtil
import com.konbini.mdbpayment.utils.LogUtils
import com.konbini.mdbpayment.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.util.ArrayList
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@AndroidEntryPoint
class MainActivity : BaseActivity(), PaymentModeAdapter.ItemListener {

    companion object {
        const val TAG = "MainActivity"
        const val MSG_MDB_INITIAL_COMPLETE = 1
        const val MSG_MDB_BEGIN_SESSION = MSG_MDB_INITIAL_COMPLETE + 1
        const val MSG_MDB_VEND = MSG_MDB_BEGIN_SESSION + 1
        const val MSG_MDB_END_SESSION = MSG_MDB_VEND + 1
    }

    private var mProcessor: MdbReaderProcessor? = null
    private var listPaymentType: MutableList<String> = mutableListOf()

    private lateinit var binding: ActivityMainBinding

    private lateinit var paymentModeAdapter: PaymentModeAdapter
    private lateinit var messageDialogFragment: MessageDialogFragment

    private val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val bdl = msg.data
            when (msg.what) {
                MSG_MDB_INITIAL_COMPLETE -> {
                    val strInfo = bdl.getString("msg")
                    messageDialogFragment.dialog?.let { dialog ->
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }
                }
                MSG_MDB_BEGIN_SESSION -> {}
                MSG_MDB_VEND -> {
                    hideIdleMode()
                    showPaymentMode()
                    setAmountValue(amount = 5.toDouble())
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
        mProcessor = MdbReaderProcessor(mHandler)
        mProcessor!!.start()

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

        setupRecyclerView()
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
        if (AppSettings.PaymentMode.Cash) {
            listPaymentType.add(PaymentModeType.CASH.value)
        }

        val coefficient = sqrt(listPaymentType.size.toDouble())
        val spanRow = coefficient.roundToInt()
        val spanCount =
            if (spanRow.toDouble().pow(2) > listPaymentType.size) spanRow else spanRow + 1
        val height = ((resources.displayMetrics.heightPixels * 0.2) / spanRow) - 5

        paymentModeAdapter = PaymentModeAdapter(this, height.toInt())
        val manager =
            GridLayoutManager(this, spanCount, GridLayoutManager.VERTICAL, false)
        binding.recyclerViewPayments.layoutManager = manager
        binding.recyclerViewPayments.adapter = paymentModeAdapter
        paymentModeAdapter.setItems(items = ArrayList(listPaymentType))
    }

    // region ================Event onClicked of Adapter================
    override fun onClickedPaymentMode(payment: String) {
        when (payment) {

        }
    }
    // endregion

    // region ================Handle UI================
    private fun setAmountValue(amount: Double) {
        binding.messageAmount.text = String.format(
            getString(R.string.message_amount_s),
            CommonUtil.formatCurrency(value = amount)
        )
    }

    private fun showIdleMode() {
        binding.idleMode.visibility = View.VISIBLE
    }

    private fun hideIdleMode() {
        binding.idleMode.visibility = View.GONE
    }

    private fun showPaymentMode() {
        binding.paymentMode.visibility = View.VISIBLE
    }

    private fun hidePaymentMode() {
        binding.paymentMode.visibility = View.GONE
    }
    // endregion
}