package com.example.pharmacistApp.activity.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.RecentActivityAdapter
import com.example.pharmacistApp.data.ActivityType
import com.example.pharmacistApp.data.DashboardData
import com.example.pharmacistApp.data.PharmaActivityType
import com.example.pharmacistApp.data.RecentActivity
import com.example.pharmacistApp.databinding.ActivityAdminDashboardBinding
import com.example.pharmacistApp.viewmodel.AdminDashboardViewModel
import com.example.pharmacistApp.viewmodel.LoadingState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val viewModel: AdminDashboardViewModel by viewModels()
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshDashboard()
        }
        swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.primaryDarkColor
        )

        setupToolbar()
        setupUI()
        setupObservers()
        viewModel.loadDashboardData()
    }

    private fun setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            title = getString(R.string.admin_dashboard)
            setDisplayHomeAsUpEnabled(false)
        }
    }

    private fun setupUI() {
        // Setup RecyclerView with improved adapter
        recentActivityAdapter = RecentActivityAdapter { activity ->
            handleActivityClick(activity)
        }

        binding.rvRecentActivity.apply {
            layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
            adapter = recentActivityAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        // Quick action button click listeners
        binding.btnViewPharmacists.setOnClickListener { navigateToPharmacistsList() }
        binding.btnViewUsers.setOnClickListener { navigateToUsersList() }
        binding.btnViewApplications.setOnClickListener { navigateToApplications() }
        binding.btnResolveComplaints.setOnClickListener { navigateToComplaints() }

        // Empty state view for recent activities
        updateEmptyState(true)
    }

    private fun setupObservers() {
        viewModel.dashboardData.observe(this) { data ->
            data?.let {
                updateStats(it)
                updateRecentActivities(it.recentActivities)
            }
        }

        viewModel.loadingState.observe(this) { state ->
            when (state) {
                is LoadingState.Loading -> showLoading(true)
                is LoadingState.Success -> {
                    showLoading(false)
                    swipeRefreshLayout.isRefreshing = false
                }
                is LoadingState.Error -> {
                    showLoading(false)
                    swipeRefreshLayout.isRefreshing = false
                    showError(state.message)
                }
            }
        }
    }

    private fun updateStats(data: DashboardData) {
        try {
            // Update Total Users card
            binding.cardTotalUsers?.apply {
                findViewById<TextView>(R.id.statTitle)?.text = getString(R.string.total_users)
                findViewById<TextView>(R.id.statValue)?.text = data.totalUsers.toString()
                findViewById<ImageView>(R.id.statIcon)?.setImageResource(R.drawable.ic_user)
            }

            // Update Total Orders card
            binding.cardTotalOrders?.apply {
                findViewById<TextView>(R.id.statTitle)?.text = getString(R.string.total_orders)
                findViewById<TextView>(R.id.statValue)?.text = data.totalOrders.toString()
                findViewById<ImageView>(R.id.statIcon)?.setImageResource(R.drawable.ic_cart)
            }

            // Update Active Pharmacies card
            binding.cardActivePharmacies?.apply {
                findViewById<TextView>(R.id.statTitle)?.text = getString(R.string.active_pharmacies)
                findViewById<TextView>(R.id.statValue)?.text = data.activePharmacies.toString()
                findViewById<ImageView>(R.id.statIcon)?.setImageResource(R.drawable.ic_pharmacy)
            }

            // Update Pending Complaints card
            binding.cardPendingComplaints?.apply {
                findViewById<TextView>(R.id.statTitle)?.text = getString(R.string.pending_complaints)
                findViewById<TextView>(R.id.statValue)?.text = data.pendingComplaints.toString()
                findViewById<ImageView>(R.id.statIcon)?.setImageResource(R.drawable.ic_error)
            }

            // Update welcome message with animation
            binding.tvWelcomeMessage.apply {
                alpha = 0f
                text = data.adminName?.let { name ->
                    getString(R.string.welcome_back_admin, name)
                } ?: getString(R.string.welcome_admin)
                animate().alpha(1f).setDuration(300).start()
            }

        } catch (e: Exception) {
            showError("Failed to update dashboard stats: ${e.message}")
        }
    }

    private fun updateRecentActivities(activities: List<RecentActivity>) {
        recentActivityAdapter.submitList(activities)
        updateEmptyState(activities.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateView?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvRecentActivity.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun handleActivityClick(activity: RecentActivity) {
        // Mark as read if it's new
        if (activity.isNew) {
            viewModel.markActivityAsRead(activity.id)
        }

        // Handle all activity types with existing navigation
        when (activity.type) {
            PharmaActivityType.NEW_USER -> navigateToUsersList()
            PharmaActivityType.NEW_ORDER -> navigateToOrdersList()
            PharmaActivityType.PHARMACY_APPLICATION -> navigateToApplications()
            PharmaActivityType.COMPLAINT -> navigateToComplaints()

            // Handle remaining cases without new activities
            PharmaActivityType.LOW_STOCK -> {
                // Reuse existing inventory view with filter
                navigateToOrdersList() // Or whichever existing view makes most sense
                showToast("Showing low stock items")
            }
            PharmaActivityType.NEW_PRESCRIPTION -> {
                // Handle within existing prescription flow
                navigateToComplaints() // Or other appropriate existing screen
                showToast("New prescription requires review")
            }
            PharmaActivityType.OTHER -> {
                // Show basic details dialog
                AlertDialog.Builder(this)
                    .setTitle(activity.title)
                    .setMessage(activity.description)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun navigateToPharmacistsList() {
        startActivity(Intent(this, PharmacistsListActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToUsersList() {
        startActivity(Intent(this, UsersListActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToApplications() {
        startActivity(Intent(this, PharmacyApplicationsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToComplaints() {
        startActivity(Intent(this, ComplaintsManagementActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToOrdersList() {
        // Implement navigation to Orders list screen
        // This is a placeholder for the future implementation
        showError("Orders list feature coming soon")
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.nestedScrollView.visibility = View.INVISIBLE
        } else {
            binding.nestedScrollView.apply {
                visibility = View.VISIBLE
                alpha = 0f
                animate().alpha(1f).setDuration(250).start()
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_admin_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshDashboard()
                true
            }
            R.id.action_settings -> {
                // Implement settings navigation
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}