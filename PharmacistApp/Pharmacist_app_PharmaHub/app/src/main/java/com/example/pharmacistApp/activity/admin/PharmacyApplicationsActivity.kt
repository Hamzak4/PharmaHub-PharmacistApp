package com.example.pharmacistApp.activity.admin

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.PharmacistApplicationsAdapter
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.databinding.ActivityPharmacyApplicationsBinding
import com.example.pharmacistApp.viewmodel.PharmacyApplicationsViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PharmacyApplicationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPharmacyApplicationsBinding
    private val viewModel: PharmacyApplicationsViewModel by viewModels()
    private lateinit var adapter: PharmacistApplicationsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPharmacyApplicationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupTabLayout()
        viewModel.loadPharmacistApplications()
    }

    private fun setupUI() {
        adapter = PharmacistApplicationsAdapter(
            onApproveClick = { pharmacist ->
                if (pharmacist.isSuspended) {
                    // For suspended pharmacists, use reinstate method
                    viewModel.reinstateSuspendedPharmacist(pharmacist.pharmacistId)
                } else {
                    // For new applications, use approve method
                    viewModel.approvePharmacist(pharmacist.pharmacistId)
                }
            },
            onRejectClick = { pharmacist ->
                viewModel.rejectPharmacist(pharmacist.pharmacistId)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PharmacyApplicationsActivity)
            adapter = this@PharmacyApplicationsActivity.adapter
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTabLayout() {
        // Add tabs for filtering between new applications and suspended pharmacists
        binding.tabLayout.apply {
            addTab(newTab().setText(R.string.new_applications))
            addTab(newTab().setText(R.string.suspended_pharmacists))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            // Show new applications
                            adapter.submitList(viewModel.pharmacists.value)
                            binding.emptyState.visibility =
                                if (viewModel.pharmacists.value?.isEmpty() == true) View.VISIBLE else View.GONE
                        }
                        1 -> {
                            // Show suspended pharmacists
                            adapter.submitList(viewModel.suspendedPharmacists.value)
                            binding.emptyState.visibility =
                                if (viewModel.suspendedPharmacists.value?.isEmpty() == true) View.VISIBLE else View.GONE
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }
    }

    private fun setupObservers() {
        // Observe new applications
        viewModel.pharmacists.observe(this) { pharmacists ->
            if (binding.tabLayout.selectedTabPosition == 0) {
                adapter.submitList(pharmacists)
                binding.emptyState.visibility = if (pharmacists.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        // Observe suspended pharmacists
        viewModel.suspendedPharmacists.observe(this) { suspendedPharmacists ->
            if (binding.tabLayout.selectedTabPosition == 1) {
                adapter.submitList(suspendedPharmacists)
                binding.emptyState.visibility = if (suspendedPharmacists.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }
}