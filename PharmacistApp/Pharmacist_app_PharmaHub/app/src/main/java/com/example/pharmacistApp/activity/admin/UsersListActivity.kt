package com.example.pharmacistApp.activity.admin

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.UsersAdapter
import com.example.pharmacistApp.databinding.ActivityUsersListBinding
import com.example.pharmacistApp.viewmodel.UsersListViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UsersListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsersListBinding
    private val viewModel: UsersListViewModel by viewModels()
    private lateinit var adapter: UsersAdapter

    companion object {
        private const val TAG = "UsersListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsersListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        Log.d(TAG, "Activity created")

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setOnRefreshListener {
                Log.d(TAG, "Refresh triggered")
                viewModel.refreshUsers()
            }
            setColorSchemeResources(
                R.color.purple_500,
                R.color.teal_200,
                R.color.g_green
            )
        }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = getString(R.string.users_list_title)
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationOnClickListener {
                Log.d(TAG, "Back button clicked")
                onBackPressed()
            }
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView")
        adapter = UsersAdapter { user ->
            Log.d(TAG, "User clicked: ${user.id}")
            // Handle click here
        }

        binding.rvUsers.apply {
            Log.d(TAG, "Configuring RecyclerView")
            layoutManager = LinearLayoutManager(this@UsersListActivity).also {
                it.stackFromEnd = false
                it.reverseLayout = false
            }
            adapter = this@UsersListActivity.adapter
            setHasFixedSize(false) // Important for proper item measurement
            itemAnimator = null // Disable animations for debugging
        }
    }

    private fun setupObservers() {
        viewModel.users.observe(this) { users ->
            Log.d(TAG, "Users data changed. Size: ${users.size}")

            binding.apply {
                tvTotalUsers.text = users.size.toString()
                tvActiveUsers.text = users.count { it.isActive }.toString()

                if (users.isEmpty()) {
                    Log.w(TAG, "No users found - showing empty state")
                    rvUsers.visibility = android.view.View.GONE
                    emptyView.visibility = android.view.View.VISIBLE
                } else {
                    Log.d(TAG, "Users available - hiding empty state")
                    rvUsers.visibility = android.view.View.VISIBLE
                    emptyView.visibility = android.view.View.GONE

                    adapter.submitList(users) {
                        Log.d(TAG, "List submitted to adapter. Item count: ${adapter.itemCount}")

                        // Debugging check
                        if (binding.rvUsers.layoutManager?.itemCount ?: 0 > 0) {
                            Log.d(TAG, "RecyclerView shows items successfully")
                        } else {
                            Log.e(TAG, "RecyclerView shows no items despite non-empty list")
                        }
                    }
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d(TAG, "Loading state changed: $isLoading")
            binding.swipeRefresh.isRefreshing = isLoading == true
        }
    }
}