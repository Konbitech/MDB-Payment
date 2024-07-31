package com.konbini.mdbpayment.ui.dialog

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.gson.Gson
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.BroadcastKey
import com.konbini.mdbpayment.R
import com.konbini.mdbpayment.data.remote.fiuu.response.DirectStatusRequeryResponse
import com.konbini.mdbpayment.databinding.FragmentQrDialogBinding
import com.konbini.mdbpayment.ui.mainActivity.MainViewModel
import com.konbini.mdbpayment.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QRDialogFragment(private val url: String, private val txnAmount: Double, private val txID: String) : DialogFragment() {
    companion object {
        const val TAG = "QRDialogFragment"
    }
    private lateinit var binding: FragmentQrDialogBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawableResource(R.drawable.custom_dialog_border)
        binding = FragmentQrDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val onCreateDialog = super.onCreateDialog(savedInstanceState)
        onCreateDialog.setCanceledOnTouchOutside(false)
        CommonUtil.hiddenSystemBars(onCreateDialog.window!!)
        return onCreateDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if (fileName.contains("svg")) {
//            val requestBuilder = Glide.with(this)
//                .`as`(PictureDrawable::class.java)
//                .fitCenter()
//                .skipMemoryCache(true)
//                .transition(withCrossFade())
//                .listener(SvgSoftwareLayerSetter())
//
//            val uri: Uri = Uri.parse(url)
//            requestBuilder.load(uri).into(binding.qrCode)
//        } else {
            Glide.with(binding.qrCode)
                .load(url)
                .placeholder(R.drawable.ic_konbini)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
                .skipMemoryCache(true)
                .error(R.drawable.ic_konbini)
                .into(binding.qrCode)
//        }

        timeoutPayment.start()

        viewLifecycleOwner.lifecycleScope.launch {
            var isSuccess = false

            while (!isSuccess) {
                delay(1000)
                val polling = viewModel.pollingDirectStatusRequery(txnAmount = String.format("%.2f", txnAmount).toDouble(), txID = txID)
                if (polling.status == Resource.Status.SUCCESS) {
                    val data = polling.data as DirectStatusRequeryResponse
                    when (data.StatCode) {
                        "00" -> {
                            isSuccess = true
                            val state = State(
                                status = Resource.Status.SUCCESS
                            )
                            sendLocalBroadcastResult(state = state)
                        }
                        "22" -> {
                            isSuccess = false
                        }
                        else -> {
                            isSuccess = true
                            val state = State(
                                status = Resource.Status.ERROR,
                                message = String.format(getString(R.string.message_error_payment), data.StatCode, data.StatName)
                            )
                            sendLocalBroadcastResult(state = state)
                        }
                    }
                }
            }
        }

        binding.buttonCancel.setSafeOnClickListener {
            val state = State(
                status = Resource.Status.ERROR,
                message = "User canceled payment"
            )
            sendLocalBroadcastResult(state = state)
        }
    }

    override fun onStart() {
        super.onStart()

        var width = 0
        var height = 0

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            width = (resources.displayMetrics.widthPixels * 0.6).toInt()
            height = (resources.displayMetrics.heightPixels * 0.8).toInt()
        } else {
            // In portrait
            width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            height = (resources.displayMetrics.heightPixels * 0.6).toInt()
        }

        dialog?.window?.setLayout(
            width,
            height
        )
    }

    private fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    // region ================Timer================
    private var timeout = AppSettings.Options.Payment.Timeout
    /**
     * Timer timeout payment
     */
    private val timeoutPayment = object : CountDownTimer(timeout * 1000, 1000) {
        override fun onTick(p0: Long) {
            if (timeout > 0) timeout -= 1
            binding.alertTimeout.text = timeout.toString()
        }

        override fun onFinish() {
            try {
                setMessage(getString(R.string.message_timeout))
                stopTimeout()

                val state = State(
                    status = Resource.Status.ERROR,
                    message = getString(R.string.message_timeout)
                )
                sendLocalBroadcastResult(state = state)
            } catch (ex: Exception) {
                LogUtils.logException(ex)
            }
        }
    }

    /**
     * Stop timeout payment
     */
    private fun stopTimeout() {
        timeoutPayment.cancel()
        if (isAdded) {
            requireActivity().runOnUiThread(Runnable {
                binding.alertTimeout.text = timeout.toString()
            })
        }
    }
    // endregion

    // region ================UI================
    private fun setMessage(message: String) {
        if (isAdded) {
            requireActivity().runOnUiThread(Runnable {
                // Update UI components here
                binding.alertMessage.text = message
            })
        }
    }
    // endregion

    private fun sendLocalBroadcastResult(state: State) {
        CommonUtil.sendLocalBroadcast(BroadcastKey.FIUU_QR_RESULT, BroadcastKey.FIUU_QR_RESULT, Gson().toJson(state))

        dismiss()
    }
}