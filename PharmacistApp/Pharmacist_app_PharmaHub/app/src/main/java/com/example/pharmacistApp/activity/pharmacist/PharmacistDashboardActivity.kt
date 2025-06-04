package com.example.pharmacistApp.activity.pharmacist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.PharmacistRecentActivityAdapter
import com.example.pharmacistApp.adapters.RecentActivityAdapter
import com.example.pharmacistApp.data.PharmaActivityType
import com.example.pharmacistApp.data.Pharmacist
import com.example.pharmacistApp.data.RecentActivity
import com.example.pharmacistApp.utils.Resource
import com.example.pharmacistApp.viewmodel.PharmacistDashboardViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class PharmacistDashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: PharmacistDashboardViewModel
    private lateinit var recentActivityRecycler: RecyclerView
    private lateinit var emptyRecentActivity: LinearLayout
    private lateinit var recentActivityAdapter: PharmacistRecentActivityAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacist_dashboard)

        // Initialize Firebase Auth and ViewModel
        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this).get(PharmacistDashboardViewModel::class.java)

        setupUI()
        setupRecyclerView()
        setupObservers()

        // Load pharmacist data when activity starts
        auth.currentUser?.uid?.let { pharmacistId ->
            viewModel.loadPharmacistData(pharmacistId)
            viewModel.loadDashboardStats(pharmacistId)
            viewModel.loadRecentActivities(pharmacistId)
        }
    }


    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
            title = ""  // Using custom title in the collapsing toolbar
        }

        // Setup drawer layout
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Setup current date
        findViewById<TextView>(R.id.txtCurrentDate).text = SimpleDateFormat(
            "EEEE, d MMMM yyyy", Locale.getDefault()
        ).format(Date())

        // Setup FAB click listener
        findViewById<ExtendedFloatingActionButton>(R.id.fabAddMedicine).setOnClickListener {
            openAddMedicineActivity(it)
        }
    }



    private fun setupRecyclerView() {
        recentActivityRecycler = findViewById(R.id.recentActivityRecycler)
        emptyRecentActivity = findViewById(R.id.emptyRecentActivity)

        recentActivityAdapter = PharmacistRecentActivityAdapter { activity ->
            when (activity.type) {
                PharmaActivityType.NEW_ORDER -> openOrdersActivity(null)
                PharmaActivityType.NEW_PRESCRIPTION -> openPrescriptionsActivity(null)
                PharmaActivityType.LOW_STOCK -> openInventoryActivity(null)
                else -> {
                    // Handle other activity types or show details
                }
            }
        }

        recentActivityRecycler.apply {
            layoutManager = LinearLayoutManager(this@PharmacistDashboardActivity)
            adapter = recentActivityAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }


    private fun setupObservers() {
        // Observe pharmacist data
        lifecycleScope.launch {
            viewModel.pharmacistData.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Show loading state if needed
                    }
                    is Resource.Success -> {
                        resource.data?.let { pharmacist ->
                            updateNavHeader(pharmacist)
                            updateDashboardWelcome(pharmacist)
                        }
                    }
                    is Resource.Error -> {
                        showDefaultNavHeader()
                        Log.e("Dashboard", "Error loading pharmacist data: ${resource.message}")
                        Toast.makeText(
                            this@PharmacistDashboardActivity,
                            "Error loading profile data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // Observe dashboard stats
        lifecycleScope.launch {
            viewModel.dashboardStats.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Show loading state
                    }
                    is Resource.Success -> {
                        resource.data?.let { stats ->
                            updateDashboardStats(stats)
                        }
                    }
                    is Resource.Error -> {
                        Log.e("Dashboard", "Error loading Stats: ${resource.message}")
                        Toast.makeText(
                            this@PharmacistDashboardActivity,
                            "Error loading dashboard stats",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // Observe recent activities
        lifecycleScope.launch {
            viewModel.recentActivities.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        // Show loading state
                    }
                    is Resource.Success -> {
                        resource.data?.let { activities ->
                            updateRecentActivities(activities)
                        }
                    }
                    is Resource.Error -> {
                        Log.e("Dashboard", "Error loading activities: ${resource.message}")
                        showEmptyRecentActivities()
                    }
                }
            }
        }
    }

    private fun updateNavHeader(pharmacist: Pharmacist) {
        val headerView = navigationView.getHeaderView(0)

        // Update name and email
        headerView.findViewById<TextView>(R.id.txtNavHeaderName).text =
            "${pharmacist.firstName} ${pharmacist.lastName}"
        headerView.findViewById<TextView>(R.id.txtNavHeaderEmail).text = pharmacist.email

        // Load profile picture with Glide
        pharmacist.profileImageUrl?.let { imageUrl ->
            Glide.with(this)
                .load(imageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(headerView.findViewById(R.id.imageView))
        } ?: run {
            headerView.findViewById<ImageView>(R.id.imageView)
                .setImageResource(R.drawable.ic_user)
        }
    }

    private fun updateDashboardWelcome(pharmacist: Pharmacist) {
        findViewById<TextView>(R.id.txtWelcome).text = "Welcome back,"
        findViewById<TextView>(R.id.txtPharmacistName).text =
            "${pharmacist.firstName} ${pharmacist.lastName}"
    }

    private fun updateDashboardStats(stats: PharmacistDashboardViewModel.DashboardStats) {
        findViewById<TextView>(R.id.txtPendingOrders).text = stats.pendingOrders.toString()
        findViewById<TextView>(R.id.txtLowStock).text = stats.lowStockItems.toString()
        findViewById<TextView>(R.id.txtPendingPrescriptions).text = stats.pendingPrescriptions.toString()
    }

    private fun updateRecentActivities(activities: List<RecentActivity>) {
        if (activities.isEmpty()) {
            showEmptyRecentActivities()
        } else {
            recentActivityRecycler.visibility = View.VISIBLE
            emptyRecentActivity.visibility = View.GONE  // Fixed typo: 60NE -> GONE
            recentActivityAdapter.submitList(activities)  // Changed from updateActivities to submitList
        }
    }

    private fun showEmptyRecentActivities() {
        recentActivityRecycler.visibility = View.GONE
        emptyRecentActivity.visibility = View.VISIBLE
    }

    private fun showDefaultNavHeader() {
        val headerView = navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.txtNavHeaderName).text =
            "Pharmacist Name"
        headerView.findViewById<TextView>(R.id.txtNavHeaderEmail).text =
            "email@example.com"
        headerView.findViewById<ImageView>(R.id.imageView)
            .setImageResource(R.drawable.ic_user)
    }

    // Handle card clicks
    fun openOrdersActivity(view: View?) {
        startActivity(Intent(this, OrdersActivity::class.java))
    }

    fun openInventoryActivity(view: View?) {
        startActivity(Intent(this, InventoryActivity::class.java))
    }

    fun openPrescriptionsActivity(view: View?) {
        startActivity(Intent(this, PrescriptionsActivity::class.java))
    }

    fun openAddMedicineActivity(view: View?) {
        startActivity(Intent(this, AddMedicineActivity::class.java))
    }

    fun openReportsActivity(view: View?) {
        startActivity(Intent(this, ReportsActivity::class.java))
    }

    fun openProcessOrderActivity(view: View?) {
        startActivity(Intent(this, ProcessOrderActivity::class.java))
    }

    fun openCustomerSupportActivity(view: View?) {
        startActivity(Intent(this, CustomerSupportOrderActivity::class.java))
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_orders -> openOrdersActivity(null)
            R.id.nav_inventory -> openInventoryActivity(null)
            R.id.nav_prescriptions -> openPrescriptionsActivity(null)
            R.id.nav_profile -> startActivity(Intent(this, ProfileActivityPharmacist::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, PharmacistLoginActivty::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }
}