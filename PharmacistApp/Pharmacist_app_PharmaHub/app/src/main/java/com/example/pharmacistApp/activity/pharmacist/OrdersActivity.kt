package com.example.pharmacistApp.activity.pharmacist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.OrderAdapter
import com.example.pharmacistApp.databinding.ActivityOrdersBinding
import com.example.pharmacistApp.data.Order
import com.example.pharmacistApp.data.OrderStatus
import com.example.pharmacistApp.data.SortOrder
import com.example.pharmacistApp.viewmodel.OrderViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrdersBinding
    private val viewModel: OrderViewModel by viewModels()
    private lateinit var orderAdapter: OrderAdapter
    private var searchJob: Job? = null

    companion object {
        private const val MENU_FILTER_ALL = -1
        private const val MENU_FILTER_DIVIDER = -2
        private const val SEARCH_DEBOUNCE_TIME_MS = 300L

        fun newIntent(context: Context) = Intent(context, OrdersActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        observeViewModel()
        loadOrders()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.orders_title)
        }
        binding.toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_more_vert)?.apply {
            setTint(ContextCompat.getColor(this@OrdersActivity, android.R.color.white))
        }
    }

    private fun setupRecyclerView() {
        orderAdapter = OrderAdapter(
            emptyList(),
            onItemClick = { order -> showOrderQuickActions(order) },
            onDetailsClick = { order ->
                startActivity(OrderDetailsActivity.newIntent(this, order.id))
            }
        )

        binding.recyclerOrders.apply {
            layoutManager = LinearLayoutManager(this@OrdersActivity)
            adapter = orderAdapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    private fun setupSearchView() {
        binding.searchView.apply {
            queryHint = getString(R.string.search_orders_hint)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String?): Boolean {
                    searchJob?.cancel()
                    searchJob = lifecycleScope.launch {
                        delay(SEARCH_DEBOUNCE_TIME_MS)
                        newText?.let {
                            if (it.length >= 3 || it.isEmpty()) {
                                viewModel.searchOrders(it)
                            }
                        }
                    }
                    return true
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.orders.observe(this) { orders ->
            orderAdapter.updateOrders(orders)
            binding.emptyState.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.error))
                    .setMessage(it)
                    .setPositiveButton(getString(R.string.ok), null)
                    .show()
            }
        }
    }

    private fun loadOrders() = viewModel.refreshOrders()

    private fun showOrderQuickActions(order: Order) {
        OrderActionsBottomSheet.newInstance(order).apply {
            setActionListener(object : OrderActionsBottomSheet.ActionListener {
                override fun onStatusChanged(orderId: String, newStatus: OrderStatus) {
                    viewModel.updateOrderStatus(orderId, newStatus)
                }

                override fun onContactCustomer(phone: String) {
                    // Handle contact logic
                }


            })
        }.show(supportFragmentManager, "OrderActions")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_orders, menu)

        (0 until menu.size()).forEach { i ->
            menu.getItem(i).icon?.mutate()?.let { icon ->
                icon.setTint(ContextCompat.getColor(this, android.R.color.white))
            }
        }

        setupStatusFilterMenu(menu)
        return true
    }

    private fun setupStatusFilterMenu(menu: Menu) {
        val filterSubMenu = menu.findItem(R.id.action_filter)?.subMenu ?: return

        filterSubMenu.clear()

        filterSubMenu.add(Menu.NONE, MENU_FILTER_ALL, Menu.NONE, getString(R.string.filter_all))
            .setIcon(R.drawable.ic_filter)

        filterSubMenu.add(Menu.NONE, MENU_FILTER_DIVIDER, Menu.NONE, "")
            .setEnabled(false)

        OrderStatus.values().forEach { status ->
            filterSubMenu.add(Menu.NONE, status.ordinal, Menu.NONE, status.displayName)
                .setIcon(status.iconRes)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_refresh -> {
                loadOrders()
                true
            }
            MENU_FILTER_ALL -> {
                viewModel.filterOrders(null)
                true
            }
            in OrderStatus.values().map { it.ordinal } -> {
                viewModel.filterOrders(OrderStatus.values()[item.itemId])
                true
            }
            R.id.action_sort -> {
                showSortOptionsDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortOptionsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort_by)
            .setItems(R.array.sort_options) { _, which ->
                when (which) {
                    0 -> viewModel.sortOrders(SortOrder.NEWEST_FIRST)
                    1 -> viewModel.sortOrders(SortOrder.OLDEST_FIRST)
                    2 -> viewModel.sortOrders(SortOrder.PRICE_HIGH_TO_LOW)
                    3 -> viewModel.sortOrders(SortOrder.PRICE_LOW_TO_HIGH)
                    4 -> viewModel.sortOrders(SortOrder.PHARMACY_NAME)
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}