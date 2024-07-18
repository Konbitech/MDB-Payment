package com.konbini.mdbpayment.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.konbini.mdbpayment.R
import com.konbini.mdbpayment.databinding.FragmentDialogMessageBinding
import com.konbini.mdbpayment.utils.CommonUtil
import com.konbini.mdbpayment.utils.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MessageDialogFragment(private val type: String, private val message: String) : DialogFragment() {
    companion object {
        const val TAG = "MessageDialogFragment"
    }

    private lateinit var binding: FragmentDialogMessageBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val onCreateDialog = super.onCreateDialog(savedInstanceState)
        onCreateDialog.setCanceledOnTouchOutside(false)
        CommonUtil.hiddenSystemBars(onCreateDialog.window!!)
        return onCreateDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(R.drawable.round_dialog)
        binding = FragmentDialogMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setIcon(type = type)
        setMessage(message = message)
    }

    override fun onStart() {
        super.onStart()

        val width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.3).toInt()

        dialog?.window?.setLayout(
            width,
            height
        )
    }

    fun isInitialized(): Boolean {
        if (this::binding.isInitialized) return true
        return false
    }

    fun setMessage(message: String) {
        binding.messageTitle.text = message
    }

    fun setIcon(type: String) {
        when (type) {
            Resource.Status.SUCCESS.name -> {
                binding.imageIcon.visibility = View.VISIBLE
                binding.spinKit.visibility = View.GONE
                binding.imageIcon.setImageResource(R.drawable.ic_success)
            }
            Resource.Status.ERROR.name -> {
                binding.imageIcon.visibility = View.VISIBLE
                binding.spinKit.visibility = View.GONE
                binding.imageIcon.setImageResource(R.drawable.ic_error)
            }
            Resource.Status.LOADING.name -> {
                binding.imageIcon.visibility = View.GONE
                binding.spinKit.visibility = View.VISIBLE
            }
        }
    }
}