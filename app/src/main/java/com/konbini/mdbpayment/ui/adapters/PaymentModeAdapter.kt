package com.konbini.mdbpayment.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.konbini.mdbpayment.AppSettings
import com.konbini.mdbpayment.MainApplication
import com.konbini.mdbpayment.R
import com.konbini.mdbpayment.data.enum.PaymentModeType
import com.konbini.mdbpayment.databinding.ItemPaymentModeBinding
import java.io.File

class PaymentModeAdapter(private val listener: ItemListener, private val heightItem: Int) : RecyclerView.Adapter<PaymentModeViewHolder>() {
    interface ItemListener {
        fun onClickedPaymentMode(payment: String)
    }

    private val items = ArrayList<String>()

    fun setItems(items: ArrayList<String>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentModeViewHolder {
        val binding: ItemPaymentModeBinding =
            ItemPaymentModeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val params: ViewGroup.LayoutParams = binding.root.layoutParams
        params.height = this.heightItem
        binding.root.layoutParams = params
        return PaymentModeViewHolder(binding, listener)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PaymentModeViewHolder, position: Int) =
        holder.bind(items[position])
}

class PaymentModeViewHolder(
    private val itemBinding: ItemPaymentModeBinding,
    private val listener: PaymentModeAdapter.ItemListener
) : RecyclerView.ViewHolder(itemBinding.root),
    View.OnClickListener {
    private lateinit var payment: String

    init {
        itemBinding.root.setOnClickListener(this)
        itemBinding.root.setOnKeyListener { _, _, _ -> true }
    }

    fun bind(payment: String) {
        this.payment = payment

        when (payment) {
            PaymentModeType.MASTER_CARD_VISA.value -> {
                if (AppSettings.PaymentMode.pathImageMasterCardVisa.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImageMasterCardVisa)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_master_visa)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_visa_mastercard)
            }
            PaymentModeType.EZ_LINK.value -> {
                if (AppSettings.PaymentMode.pathImageEzLink.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImageEzLink)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_ezlink)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_ez_link)
            }
            PaymentModeType.PAYNOW.value -> {
                if (AppSettings.PaymentMode.pathImagePayNow.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImagePayNow)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_pay_now)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_pay_now)
            }
            PaymentModeType.ALIPAY.value -> {
                if (AppSettings.PaymentMode.pathImageAliPay.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImageAliPay)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_alipay)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_ali_pay)
            }
            PaymentModeType.GRABPAY.value -> {
                if (AppSettings.PaymentMode.pathImageGrabPay.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImageGrabPay)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_grabpay)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_grab_pay)//"GrabPay"
            }
            PaymentModeType.WECHAT.value -> {
                if (AppSettings.PaymentMode.pathImageWeChat.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImageWeChat)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_wechat)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_we_chat)//"WeChat"
            }
            PaymentModeType.KONBI_WALLET.value -> {
                if (AppSettings.PaymentMode.pathImageKonbiniWallet.isNotEmpty()) {
                    val imgFile = File(AppSettings.PaymentMode.pathImageKonbiniWallet)
                    if (imgFile.exists()) {
                        setIcon(imgFile)
                    }
                } else {
                    setIcon(R.drawable.ic_konbini)
                }
                itemBinding.paymentName.text = MainApplication.instance.resources.getString(R.string.title_wallet)//"Wallet"
            }
        }
    }

    private fun setIcon(imgFile: File) {
        if (imgFile.exists()) {
            Glide.with(itemView)
                .load(imgFile)
                .into(itemBinding.btnCardType)
        }
    }

    private fun setIcon(id: Int) {
        Glide.with(itemView)
            .load(id)
            .into(itemBinding.btnCardType)
    }

    override fun onClick(p0: View?) {
        listener.onClickedPaymentMode(payment)
    }
}