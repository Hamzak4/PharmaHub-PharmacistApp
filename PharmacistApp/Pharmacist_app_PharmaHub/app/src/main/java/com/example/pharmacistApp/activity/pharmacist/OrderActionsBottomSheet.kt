package com.example.pharmacistApp.activity.pharmacist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Order
import com.example.pharmacistApp.data.OrderStatus
import com.example.pharmacistApp.databinding.BottomSheetOrderActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OrderActionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOrderActionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var order: Order
    private var listener: ActionListener? = null

    interface ActionListener {
        fun onStatusChanged(orderId: String, newStatus: OrderStatus)
        fun onContactCustomer(phone: String)
    }

    companion object {
        private const val ARG_ORDER = "order"
        fun newInstance(order: Order) = OrderActionsBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_ORDER, order) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetOrderActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        order = arguments?.getParcelable(ARG_ORDER) ?: return
        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        binding.apply {
            tvOrderId.text = getString(R.string.order_id_format, order.id)
            tvCustomerName.text = order.customerName
            tvStatus.text = order.status.name
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnContact.setOnClickListener { listener?.onContactCustomer(order.customerPhone); dismiss() }
            btnApprove.setOnClickListener { listener?.onStatusChanged(order.id, OrderStatus.ORDERED); dismiss() }
            btnReject.setOnClickListener { listener?.onStatusChanged(order.id, OrderStatus.CANCELLED); dismiss() }
            btnPrepare.setOnClickListener { listener?.onStatusChanged(order.id, OrderStatus.PROCESSING); dismiss() }
            btnOutForDelivery.setOnClickListener { listener?.onStatusChanged(order.id, OrderStatus.SHIPPED); dismiss() }
            btnDelivered.setOnClickListener { listener?.onStatusChanged(order.id, OrderStatus.DELIVERED); dismiss() }
        }
    }

    fun setActionListener(listener: ActionListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}