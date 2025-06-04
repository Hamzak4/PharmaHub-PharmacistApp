// Enhanced InventoryActivity class with improved filtering and search capabilities

package com.example.pharmacistApp.activity.pharmacist

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.MedicineInventoryAdapter
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.databinding.ActivityInventoryBinding
import com.example.pharmacistApp.utils.Resource
import com.example.pharmacistApp.viewmodel.InventoryViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InventoryActivity : AppCompatActivity(), MedicineInventoryAdapter.OnMedicineActionListener {

    private lateinit var binding: ActivityInventoryBinding
    private lateinit var viewModel: InventoryViewModel
    private lateinit var adapter: MedicineInventoryAdapter
    private lateinit var auth: FirebaseAuth
    private var currentFilter: String = "all" // Track current filter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        viewModel = ViewModelProvider(this).get(InventoryViewModel::class.java)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Load medicines for current pharmacist
        auth.currentUser?.uid?.let { pharmacistId ->
            viewModel.loadMedicines(pharmacistId)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pharmacy Inventory"
    }

    private fun setupRecyclerView() {
        adapter = MedicineInventoryAdapter(this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@InventoryActivity)
            adapter = this@InventoryActivity.adapter
            setHasFixedSize(true) // Optimization for better performance
        }
    }

    private fun setupListeners() {
        // Improved search functionality with TextWatcher
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()

                // Only search if we have at least 2 chars or empty query to reset
                if (query.length >= 2) {
                    viewModel.searchMedicines(query)
                } else if (query.isEmpty()) {
                    // Reset to previous filter or show all
                    applyCurrentFilter()
                }
            }
        })

        // Filter button with feedback
        binding.btnFilter.setOnClickListener {
            showFilterOptions(it)
        }

        // Add new medicine button
        binding.fabAddMedicine.setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

        // Click listeners for stats cards (quick filtering)
        binding.txtLowStock.setOnClickListener {
            auth.currentUser?.uid?.let { pharmacistId ->
                binding.searchEditText.text?.clear()
                currentFilter = "low_stock"
                viewModel.filterByLowStock(pharmacistId)
            }
        }

        binding.txtOutOfStock.setOnClickListener {
            auth.currentUser?.uid?.let { pharmacistId ->
                binding.searchEditText.text?.clear()
                currentFilter = "out_of_stock"
                viewModel.filterByOutOfStock(pharmacistId)
            }
        }

        binding.txtTotalMedicines.setOnClickListener {
            auth.currentUser?.uid?.let { pharmacistId ->
                binding.searchEditText.text?.clear()
                currentFilter = "all"
                viewModel.loadMedicines(pharmacistId)
            }
        }
    }

    private fun applyCurrentFilter() {
        auth.currentUser?.uid?.let { pharmacistId ->
            when (currentFilter) {
                "all" -> viewModel.loadMedicines(pharmacistId)
                "low_stock" -> viewModel.filterByLowStock(pharmacistId)
                "out_of_stock" -> viewModel.filterByOutOfStock(pharmacistId)
                "prescription" -> viewModel.filterByPrescriptionRequired(pharmacistId)
                "recent" -> viewModel.filterByRecentlyAdded(pharmacistId)
                else -> {
                    // Handle category filters
                    if (currentFilter.startsWith("category_")) {
                        val category = currentFilter.removePrefix("category_")
                        viewModel.filterByCategory(pharmacistId, category)
                    } else {
                        viewModel.loadMedicines(pharmacistId)
                    }
                }
            }
        }
    }

    private fun showFilterOptions(view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_inventory_filter, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            binding.searchEditText.text?.clear() // Clear search when filtering

            when (item.itemId) {
                R.id.filter_all -> {
                    currentFilter = "all"
                    auth.currentUser?.uid?.let { pharmacistId ->
                        viewModel.loadMedicines(pharmacistId)
                    }
                    true
                }
                R.id.filter_low_stock -> {
                    currentFilter = "low_stock"
                    auth.currentUser?.uid?.let { pharmacistId ->
                        viewModel.filterByLowStock(pharmacistId)
                    }
                    true
                }
                R.id.filter_out_of_stock -> {
                    currentFilter = "out_of_stock"
                    auth.currentUser?.uid?.let { pharmacistId ->
                        viewModel.filterByOutOfStock(pharmacistId)
                    }
                    true
                }
                R.id.filter_by_category -> {
                    showCategoryFilterOptions(view)
                    true
                }
                R.id.filter_prescription_only -> {
                    currentFilter = "prescription"
                    auth.currentUser?.uid?.let { pharmacistId ->
                        viewModel.filterByPrescriptionRequired(pharmacistId)
                    }
                    true
                }
                R.id.filter_recently_added -> {
                    currentFilter = "recent"
                    auth.currentUser?.uid?.let { pharmacistId ->
                        viewModel.filterByRecentlyAdded(pharmacistId)
                    }
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showCategoryFilterOptions(view: View) {
        val categories = resources.getStringArray(R.array.product_categories)
        val popup = PopupMenu(this, view)

        categories.forEachIndexed { index, category ->
            popup.menu.add(Menu.NONE, 1000 + index, index, category)
        }

        popup.setOnMenuItemClickListener { item ->
            val categoryIndex = item.itemId - 1000
            if (categoryIndex in categories.indices) {
                val selectedCategory = categories[categoryIndex]
                currentFilter = "category_$selectedCategory"
                auth.currentUser?.uid?.let { pharmacistId ->
                    viewModel.filterByCategory(pharmacistId, selectedCategory)
                }
                true
            } else {
                false
            }
        }
        popup.show()
    }

    private fun observeViewModel() {
        viewModel.medicines.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyState.visibility = View.GONE
                    Log.d("InventoryActivity", "Loading medicines...")
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE

                    resource.data?.let { medicines ->
                        // Add detailed logging to see the data
                        Log.d("InventoryActivity", "Received ${medicines.size} medicines")
                        if (medicines.isNotEmpty()) {
                            medicines.forEach { product ->
                                // Log each product to check their contents
                                Log.d("InventoryActivity", "Product: ${product.id} - ${product.name}, Quantity: ${product.quantity}")
                            }
                        } else {
                            Log.d("InventoryActivity", "Medicine list is empty")
                        }

                        if (medicines.isEmpty()) {
                            binding.recyclerView.visibility = View.GONE
                            binding.emptyState.visibility = View.VISIBLE

                            // Show appropriate empty state message based on filter
                            updateEmptyStateMessage()
                        } else {
                            binding.recyclerView.visibility = View.VISIBLE
                            binding.emptyState.visibility = View.GONE
                            adapter.submitList(medicines)
                            updateInventoryStats(medicines)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE

                    // Enhanced error logging
                    Log.e("InventoryActivity", "Error loading medicines: ${resource.message}")
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateEmptyStateMessage() {
        // Update empty state message based on current filter
        val message = when (currentFilter) {
            "low_stock" -> "No medicines with low stock"
            "out_of_stock" -> "No out of stock medicines"
            "prescription" -> "No prescription medicines found"
            "recent" -> "No recently added medicines"
            else -> {
                if (currentFilter.startsWith("category_")) {
                    "No medicines in this category"
                } else if (binding.searchEditText.text.toString().isNotEmpty()) {
                    "No search results found"
                } else {
                    "No medicines in inventory"
                }
            }
        }

        // Update the message in the empty state view
        val emptyMessageView = binding.emptyState.findViewById<android.widget.TextView>(R.id.empty_state_message)
        emptyMessageView?.text = message ?: "No medicines found"
    }

    private fun updateInventoryStats(medicines: List<Product>) {
        // These stats should be calculated from the TOTAL medicines, not just the filtered ones
        // For this, we should ideally have a separate LiveData for stats
        // But as a quick fix, we'll keep the original list cached in ViewModel

        // For now, we're calculating based on the current list
        val totalMedicines = medicines.size
        val lowStock = medicines.count { it.quantity > 0 && it.quantity <= 10 }
        val outOfStock = medicines.count { it.quantity <= 0 }

        binding.txtTotalMedicines.text = totalMedicines.toString()
        binding.txtLowStock.text = lowStock.toString()
        binding.txtOutOfStock.text = outOfStock.toString()
    }

    override fun onMedicineEdit(product: Product) {
        val intent = Intent(this, UpdateMedicineActivity::class.java)
        // Change the key to match what UpdateMedicineActivity expects
        intent.putExtra(UpdateMedicineActivity.KEY_PRODUCT_ID, product.id)
        startActivity(intent)
    }


    override fun onMedicineDelete(product: Product) {
        // Show confirmation dialog before deleting
        AlertDialog.Builder(this)
            .setTitle("Delete Medicine")
            .setMessage("Are you sure you want to delete ${product.name}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMedicine(product.id)
                Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this activity
        auth.currentUser?.uid?.let { pharmacistId ->
            applyCurrentFilter()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}