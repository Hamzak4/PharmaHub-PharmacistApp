package com.example.pharmacistApp.activity.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.PharmacistsAdapter
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.databinding.ActivityPharmacistsListBinding
import com.example.pharmacistApp.viewmodel.PharmacistsListViewModel
import com.example.pharmacistApp.viewmodel.PharmacistsListViewModel.OperationStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PharmacistsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPharmacistsListBinding
    private val viewModel: PharmacistsListViewModel by viewModels()
    private lateinit var adapter: PharmacistsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPharmacistsListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_approved_pharmacists)
        }

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_filter -> {
                    showFilterDialog()
                    true
                }
                R.id.menu_export -> {
                    exportData()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = PharmacistsAdapter(
            onItemClick = { pharmacist -> showPharmacistDetails(pharmacist) },
            onRemoveClick = { pharmacist -> showRemovalOptions(pharmacist) }
        )

        binding.rvPharmacists.apply {
            layoutManager = LinearLayoutManager(this@PharmacistsListActivity)
            adapter = this@PharmacistsListActivity.adapter
            addItemDecoration(
                DividerItemDecoration(
                    this@PharmacistsListActivity,
                    DividerItemDecoration.VERTICAL
                ).apply {
                    ContextCompat.getDrawable(
                        this@PharmacistsListActivity,
                        R.drawable.divider
                    )?.let { setDrawable(it) }
                }
            )
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.purple_500),
            ContextCompat.getColor(this, R.color.teal_200)
        )
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    private fun setupObservers() {
        viewModel.pharmacists.observe(this) { pharmacists ->
            adapter.submitList(pharmacists)
            binding.tvEmptyState.visibility = if (pharmacists.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.operationStatus.observe(this) { status ->
            when (status) {
                is OperationStatus.Success -> {
                    showToast(status.message)
                    loadData() // Refresh after operation
                }
                is OperationStatus.Error -> {
                    showToast(status.message, isError = true)
                    binding.swipeRefresh.isRefreshing = false
                }
                is OperationStatus.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    private fun loadData() {
        viewModel.loadApprovedPharmacists()
    }

    private fun showPharmacistDetails(pharmacist: Pharmacist) {
        AlertDialog.Builder(this)
            .setTitle("Pharmacist Details")
            .setMessage(
                """
                Name: ${pharmacist.firstName} ${pharmacist.lastName}
                Pharmacy: ${pharmacist.pharmacyName}
                License: ${pharmacist.licenseNumber}
                Email: ${pharmacist.email}
                Phone: ${pharmacist.phoneNumber}
                Address: ${pharmacist.address}, ${pharmacist.city}, ${pharmacist.state} ${pharmacist.zipCode}
                """.trimIndent()
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showRemovalOptions(pharmacist: Pharmacist) {
        val options = arrayOf(
            getString(R.string.option_deactivate),
            getString(R.string.option_delete),
            getString(R.string.option_suspend)
        )

        AlertDialog.Builder(this)
            .setTitle("Remove Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.deactivatePharmacist(pharmacist.pharmacistId)
                    1 -> confirmPermanentDeletion(pharmacist)
                    2 -> suspendPharmacist(pharmacist.pharmacistId)
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun confirmPermanentDeletion(pharmacist: Pharmacist) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_confirm_deletion))
            .setMessage(getString(R.string.msg_confirm_remove, pharmacist.pharmacyName))
            .setPositiveButton(getString(R.string.btn_confirm)) { _, _ ->
                viewModel.deletePharmacist(pharmacist.pharmacistId)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun suspendPharmacist(pharmacistId: String) {
        viewModel.suspendPharmacist(pharmacistId, days = 30) // 30-day suspension
    }

    private fun showFilterDialog() {
        val filters = arrayOf(
            getString(R.string.filter_all),
            getString(R.string.filter_active),
            getString(R.string.filter_inactive),
            getString(R.string.filter_suspended)
        )

        AlertDialog.Builder(this)
            .setTitle("Filter Pharmacists")
            .setSingleChoiceItems(filters, viewModel.currentFilter.value ?: 0) { dialog, which ->
                viewModel.applyFilter(which)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun exportData() {
        viewModel.exportPharmacistData().observe(this) { result ->
            when (result) {
                is OperationStatus.Success -> {
                    showToast("Data exported successfully")
                }
                is OperationStatus.Error -> {
                    showToast("Export failed: ${result.message}", isError = true)
                }
                is OperationStatus.Loading -> {
                    // Show loading indicator
                }
            }
        }
    }

    private fun showToast(message: String, isError: Boolean = false) {
        try {
            val duration = Toast.LENGTH_LONG
            val toast = Toast.makeText(this, message, duration)
            toast.show()
        } catch (e: Exception) {
            Log.e("ToastError", "Failed to show toast", e)
        }
    }

    companion object {
        private const val TAG = "PharmacistsListActivity"
    }
}