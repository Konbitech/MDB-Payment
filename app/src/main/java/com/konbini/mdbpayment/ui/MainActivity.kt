package com.konbini.mdbpayment.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.konbini.mdbpayment.R
import com.konbini.mdbpayment.databinding.ActivityMainBinding
import com.konbini.mdbpayment.hardware.MdbReaderProcessor
import com.konbini.mdbpayment.ui.dialog.MessageDialogFragment
import com.konbini.mdbpayment.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val MSG_MDB_INITIAL_COMPLETE = 1
        const val MSG_MDB_BEGIN_SESSION = MSG_MDB_INITIAL_COMPLETE + 1
        const val MSG_MDB_VEND = MSG_MDB_BEGIN_SESSION + 1
        const val MSG_MDB_END_SESSION = MSG_MDB_VEND + 1
    }

    private var mProcessor: MdbReaderProcessor? = null
    private lateinit var binding: ActivityMainBinding

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
    }
}