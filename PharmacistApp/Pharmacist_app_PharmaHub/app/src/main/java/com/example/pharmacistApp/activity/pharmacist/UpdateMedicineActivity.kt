package com.example.pharmacistApp.activity.pharmacist

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.databinding.ActivityUpdateMedicineBinding
import com.example.pharmacistApp.utils.Resource
import com.example.pharmacistApp.viewmodel.UpdateMedicineViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdateMedicineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateMedicineBinding
    private val viewModel: UpdateMedicineViewModel by viewModels()
    private var productId: String? = null
    private var selectedImageUri: Uri? = null
    private var currentProduct: Product? = null

    // Category items
    private lateinit var categories: Array<String>
    private lateinit var dosageForms: Array<String>

    private var progressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateMedicineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Update Medicine"

        // Get product ID from intent
        productId = intent.getStringExtra(KEY_PRODUCT_ID)
        if (productId.isNullOrEmpty()) {
            Toast.makeText(this, "Product ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize arrays
        categories = resources.getStringArray(R.array.product_categories)
        dosageForms = arrayOf("Tablet", "Capsule", "Syrup", "Injection", "Ointment", "Drops")

        setupSpinners()
        setupClickListeners()
        setupBackPressedCallback()
        observeViewModel()

        // Load product data
        viewModel.loadProductDetails(productId!!)
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                checkForUnsavedChanges()
            }
        })
    }

    private fun setupSpinners() {
        // Setup category spinner
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        (binding.spinnerCategory as? AutoCompleteTextView)?.setAdapter(categoryAdapter)

        // Setup dosage form spinner
        val dosageFormAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            dosageForms
        )
        (binding.spinnerDosageForm as? AutoCompleteTextView)?.setAdapter(dosageFormAdapter)
    }

    private fun setupClickListeners() {
        // Image selection
        binding.fabSelectImage.setOnClickListener {
            openImagePicker()
        }

        // Alternative image selection button
        binding.fabSelectImage.setOnClickListener {
            openImagePicker()
        }

        // Update button
        binding.btnUpdate.setOnClickListener {
            if (validateInputs()) {
                updateMedicine()
            }
        }

        // Edit active ingredients
        binding.btnEditIngredients.setOnClickListener {
            editActiveIngredients()
        }

        // Edit strengths
        binding.btnEditStrengths.setOnClickListener {
            editStrengths()
        }

        // Quick save button
        binding.fabQuickSave.setOnClickListener {
            if (validateInputs()) {
                updateMedicine()
            }
        }
    }

    private fun observeViewModel() {
        // Observe product details loading
        viewModel.productDetails.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading(true)
                }
                is Resource.Success -> {
                    showLoading(false)
                    resource.data?.let { product ->
                        populateProductData(product)
                    }
                }
                is Resource.Error -> {
                    showLoading(false)
                    Toast.makeText(this, "Error: ${resource.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }

        // Observe update status
        viewModel.updateStatus.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showUpdateProgress(true)
                }
                is Resource.Success -> {
                    showUpdateProgress(false)
                    Toast.makeText(this, "Medicine updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is Resource.Error -> {
                    showUpdateProgress(false)
                    Toast.makeText(this, "Update failed: ${resource.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun populateProductData(product: Product) {
        currentProduct = product

        // Set text fields
        binding.apply {
            edtMedicineName.setText(product.name)
            edtGenericName.setText(product.genericName)
            edtPrice.setText(product.price.toString())
            edtQuantity.setText(product.quantity.toString())
            edtDescription.setText(product.description ?: "")
            edtManufacturer.setText(product.manufacturer ?: "")
            edtSideEffects.setText(product.sideEffects ?: "")
            edtStorageInstructions.setText(product.storageInstructions ?: "")

            // Set discount if available
            product.offerPercentage?.let {
                edtDiscountPercentage.setText(it.toString())
            }

            // Set switch for prescription
            switchRequiresPrescription.isChecked = product.requiresPrescription

            // Set spinners
            val categoryIndex = categories.indexOf(product.category)
            if (categoryIndex >= 0) {
                (spinnerCategory as? AutoCompleteTextView)?.setText(product.category, false)
            }

            val dosageFormIndex = dosageForms.indexOf(product.dosageForm)
            if (dosageFormIndex >= 0) {
                (spinnerDosageForm as? AutoCompleteTextView)?.setText(product.dosageForm, false)
            }

            // Display first image if available
            if (product.images.isNotEmpty()) {
                imgPlaceholder.visibility = View.GONE
                Glide.with(this@UpdateMedicineActivity)
                    .load(product.images[0])
                    .placeholder(R.drawable.ic_medicine)
                    .into(imgMedicine)
            } else {
                imgPlaceholder.visibility = View.VISIBLE
            }

            // Display active ingredients
            tvActiveIngredients.text = product.activeIngredients.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") ?: "None"

            // Display strengths
            tvStrengths.text = product.strengths.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") ?: "None"

            // Set title
            collapsingToolbar.title = product.name
        }
    }

    private fun openImagePicker() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            startActivityForResult(this, PICK_IMAGE_REQUEST)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedImageUri = uri
                binding.imgPlaceholder.visibility = View.GONE
                Glide.with(this)
                    .load(uri)
                    .into(binding.imgMedicine)
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        with(binding) {
            if (edtMedicineName.text.isNullOrBlank()) {
                edtMedicineName.error = "Medicine name is required"
                isValid = false
            }

            if (edtGenericName.text.isNullOrBlank()) {
                edtGenericName.error = "Generic name is required"
                isValid = false
            }

            if (edtPrice.text.isNullOrBlank()) {
                edtPrice.error = "Price is required"
                isValid = false
            } else if (edtPrice.text.toString().toFloatOrNull()?.takeIf { it >= 0 } == null) {
                edtPrice.error = "Invalid price value"
                isValid = false
            }

            if (edtQuantity.text.isNullOrBlank()) {
                edtQuantity.error = "Quantity is required"
                isValid = false
            } else if (edtQuantity.text.toString().toIntOrNull()?.takeIf { it >= 0 } == null) {
                edtQuantity.error = "Invalid quantity"
                isValid = false
            }

            // Validate discount percentage if provided
            if (!edtDiscountPercentage.text.isNullOrBlank()) {
                val discount = edtDiscountPercentage.text.toString().toFloatOrNull()
                if (discount == null || discount !in 0f..100f) {
                    edtDiscountPercentage.error = "Discount must be between 0-100"
                    isValid = false
                }
            }
        }

        return isValid
    }

    private fun updateMedicine() {
        val currentProduct = this.currentProduct ?: return

        // Get values directly from the AutoCompleteTextView
        val categoryText = (binding.spinnerCategory as? AutoCompleteTextView)?.text.toString()
        val dosageFormText = (binding.spinnerDosageForm as? AutoCompleteTextView)?.text.toString()

        // Use entered text or fall back to current product values
        val category = if (categoryText.isNotBlank()) categoryText else currentProduct.category
        val dosageForm = if (dosageFormText.isNotBlank()) dosageFormText else currentProduct.dosageForm

        // Create updated product object
        val updatedProduct = currentProduct.copy(
            name = binding.edtMedicineName.text.toString().trim(),
            genericName = binding.edtGenericName.text.toString().trim(),
            category = category,
            dosageForm = dosageForm,
            price = binding.edtPrice.text.toString().toFloatOrNull() ?: 0f,
            quantity = binding.edtQuantity.text.toString().toIntOrNull() ?: 0,
            offerPercentage = binding.edtDiscountPercentage.text.toString().toFloatOrNull(),
            description = binding.edtDescription.text.toString().trim().takeIf { it.isNotBlank() },
            manufacturer = binding.edtManufacturer.text.toString().trim().takeIf { it.isNotBlank() },
            sideEffects = binding.edtSideEffects.text.toString().trim().takeIf { it.isNotBlank() },
            storageInstructions = binding.edtStorageInstructions.text.toString().trim().takeIf { it.isNotBlank() },
            requiresPrescription = binding.switchRequiresPrescription.isChecked
        )

        // Update product via viewModel
        viewModel.updateProduct(updatedProduct, selectedImageUri)
    }

    private fun editActiveIngredients() {
        val currentProduct = this.currentProduct ?: return
        val currentIngredients = currentProduct.activeIngredients.toMutableList()

        // Show dialog to edit active ingredients
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Active Ingredients")
            .setMessage("Feature coming soon. This will allow you to add, edit, and remove active ingredients.")
            .setPositiveButton("OK", null)
            .show()

        // TODO: Implement proper editor for active ingredients
    }

    private fun editStrengths() {
        val currentProduct = this.currentProduct ?: return
        val currentStrengths = currentProduct.strengths.toMutableList()

        // Show dialog to edit strengths
        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Strengths")
            .setMessage("Feature coming soon. This will allow you to add, edit, and remove strength values.")
            .setPositiveButton("OK", null)
            .show()

        // TODO: Implement proper editor for strengths
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.mainContent.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showUpdateProgress(isLoading: Boolean) {
        if (isLoading) {
            if (progressDialog == null) {
                progressDialog = Dialog(this).apply {
                    setContentView(R.layout.dialog_loading)
                    setCancelable(false)
                }
            }
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Ask for confirmation if data has been changed
            checkForUnsavedChanges()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkForUnsavedChanges() {
        // TODO: Implement check for changes
        // For now, just show a confirmation dialog
        MaterialAlertDialogBuilder(this)
            .setTitle("Discard changes?")
            .setMessage("You have unsaved changes. Are you sure you want to leave this screen?")
            .setPositiveButton("Discard") { _, _ -> finish() }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    companion object {
        const val KEY_PRODUCT_ID = "product_id"
        private const val PICK_IMAGE_REQUEST = 102
    }
}