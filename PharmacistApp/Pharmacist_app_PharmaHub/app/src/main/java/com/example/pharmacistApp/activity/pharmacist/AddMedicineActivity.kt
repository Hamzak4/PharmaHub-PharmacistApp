package com.example.pharmacistApp.activity.pharmacist

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.SelectedImagesAdapter
import com.example.pharmacistApp.cloudinary.CloudinaryHelper
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddMedicineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val selectedImages = mutableListOf<Uri>()
    private val imageAdapter = SelectedImagesAdapter()
    private val dosageForms = listOf("Tablet", "Capsule", "Syrup", "Injection", "Ointment", "Drops")
    private val strengths = mutableListOf<String>()
    private val activeIngredients = mutableListOf<String>()

    // Pharmacist data variables
    private var pharmacistId: String = ""
    private var pharmacyName: String = ""
    private var pharmacyAddress: String = ""
    private var isPharmacistVerified = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Get current user ID
        pharmacistId = auth.currentUser?.uid ?: ""

        if (pharmacistId.isEmpty()) {
            Toast.makeText(this, "Pharmacist not authenticated", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Verify pharmacist status and get pharmacy info
        verifyPharmacistAndGetInfo()

        CloudinaryHelper.initialize(this)
        setupViews()
        setupClickListeners()
    }

    private fun verifyPharmacistAndGetInfo() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Verifying pharmacist...")
            setCancelable(false)
            show()
        }

        db.collection("pharmacists").document(pharmacistId)
            .get()
            .addOnSuccessListener { document ->
                progressDialog.dismiss()

                if (!document.exists()) {
                    Toast.makeText(this, "Pharmacist profile not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val isApproved = document.getBoolean("status") ?: false
                val isSuspended = document.getBoolean("isSuspended") ?: false

                if (!isApproved) {
                    Toast.makeText(this, "Account not approved", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                if (isSuspended) {
                    Toast.makeText(this, "Account suspended", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                // Get pharmacy information
                pharmacyName = document.getString("pharmacyName") ?: ""
                pharmacyAddress = document.getString("fullAddress") ?: ""

                if (pharmacyName.isEmpty() || pharmacyAddress.isEmpty()) {
                    Toast.makeText(this, "Pharmacy information incomplete", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                isPharmacistVerified = true
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to verify pharmacist", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupViews() {
        // Setup category dropdown
        val categories = resources.getStringArray(R.array.product_categories)
        val categoryAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            categories
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.autoCompleteCategory.apply {
            setAdapter(categoryAdapter)
            threshold = 1
            setOnItemClickListener { _, _, _, _ ->
                binding.categoryInputLayout.error = null
            }
        }

        // Setup dosage form dropdown
        val dosageFormAdapter = ArrayAdapter(
            this,
            R.layout.dropdown_menu_item,
            dosageForms
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.autoCompleteDosageForm.apply {
            setAdapter(dosageFormAdapter)
            threshold = 1
            setOnItemClickListener { _, _, _, _ ->
                binding.dosageFormInputLayout.error = null
            }
        }

        // Setup images recyclerview
        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(
                this@AddMedicineActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = imageAdapter
        }

        // Setup quantity input
        binding.edQuantity.apply {
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus && text.isNullOrBlank()) {
                    setText("1")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonAddStrength.setOnClickListener { addStrength() }
        binding.buttonAddIngredient.setOnClickListener { addActiveIngredient() }
        binding.buttonImagesPicker.setOnClickListener { openImagePicker() }
        binding.btnSubmit.setOnClickListener {
            if (!isPharmacistVerified) {
                Toast.makeText(this, "Please wait while we verify your account", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (validateForm()) saveProduct()
        }
    }

    private fun addStrength() {
        binding.edStrength.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
            strengths.add(it)
            binding.edStrength.text?.clear()
            updateSelectedStrengths()
        } ?: showToast("Please enter a valid strength", true)
    }

    private fun addActiveIngredient() {
        binding.edActiveIngredients.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let {
            activeIngredients.add(it)
            binding.edActiveIngredients.text?.clear()
            updateSelectedIngredients()
        } ?: showToast("Please enter a valid ingredient", true)
    }

    private fun updateSelectedStrengths() {
        binding.tvSelectedStrengths.text = strengths.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ?: "No strengths added"
    }

    private fun updateSelectedIngredients() {
        binding.tvSelectedIngredients.text = activeIngredients.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ?: "No ingredients added"
    }

    private fun openImagePicker() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(this, PICK_IMAGES_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            when {
                data?.clipData != null -> {
                    (0 until data.clipData!!.itemCount).map { i ->
                        data.clipData!!.getItemAt(i).uri
                    }.filterNotNull().let { uris ->
                        selectedImages.addAll(uris)
                    }
                }
                data?.data != null -> selectedImages.add(data.data!!)
            }
            updateSelectedImages()
        }
    }

    private fun updateSelectedImages() {
        binding.rvSelectedImages.visibility = if (selectedImages.isNotEmpty()) View.VISIBLE else View.GONE
        imageAdapter.submitList(selectedImages.toList())
    }

    private fun validateForm(): Boolean {
        var isValid = true

        with(binding) {
            if (edName.text.isNullOrBlank()) {
                edName.error = "Brand name is required"
                isValid = false
            }

            if (edGenericName.text.isNullOrBlank()) {
                edGenericName.error = "Generic name is required"
                isValid = false
            }

            if (autoCompleteCategory.text.isNullOrBlank()) {
                categoryInputLayout.error = "Category is required"
                isValid = false
            }

            if (autoCompleteDosageForm.text.isNullOrBlank()) {
                dosageFormInputLayout.error = "Dosage form is required"
                isValid = false
            }

            if (strengths.isEmpty()) {
                showToast("Please add at least one strength", true)
                isValid = false
            }

            if (edPrice.text.isNullOrBlank()) {
                edPrice.error = "Price is required"
                isValid = false
            } else if (edPrice.text.toString().toFloatOrNull()?.takeIf { it >= 0 } == null) {
                edPrice.error = "Invalid price value"
                isValid = false
            }

            if (edQuantity.text.isNullOrBlank()) {
                edQuantity.error = "Quantity is required"
                isValid = false
            } else if (edQuantity.text.toString().toIntOrNull()?.takeIf { it > 0 } == null) {
                edQuantity.error = "Quantity must be at least 1"
                isValid = false
            }

            binding.offerPercentage.text?.toString()?.toFloatOrNull()?.takeIf { it !in 0f..100f }?.let {
                offerPercentage.error = "Discount must be between 0-100"
                isValid = false
            }
        }

        return isValid
    }

    private fun saveProduct() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Saving medicine...")
            setCancelable(false)
            show()
        }

        if (selectedImages.isNotEmpty()) {
            uploadImagesToCloudinary(progressDialog)
        } else {
            saveProductToFirestore(emptyList(), progressDialog)
        }
    }

    private fun uploadImagesToCloudinary(progressDialog: ProgressDialog) {
        val imageUrls = mutableListOf<String>()
        var uploadCount = 0

        selectedImages.forEach { uri ->
            CloudinaryHelper.uploadImage(
                uri,
                onSuccess = { url ->
                    imageUrls.add(url)
                    if (++uploadCount == selectedImages.size) {
                        saveProductToFirestore(imageUrls, progressDialog)
                    }
                },
                onError = { error ->
                    progressDialog.dismiss()
                    showToast("Image upload failed: $error", true)
                }
            )
        }
    }

    private fun saveProductToFirestore(imageUrls: List<String>, progressDialog: ProgressDialog) {
        val quantity = binding.edQuantity.text.toString().toIntOrNull() ?: 1

        val product = Product(
            id = UUID.randomUUID().toString(),
            name = binding.edName.text?.toString()?.trim() ?: "",
            genericName = binding.edGenericName.text?.toString()?.trim() ?: "",
            category = binding.autoCompleteCategory.text?.toString()?.trim() ?: "Uncategorized",
            price = binding.edPrice.text?.toString()?.toFloatOrNull() ?: 0f,
            offerPercentage = binding.offerPercentage.text?.toString()?.toFloatOrNull()
                ?.takeIf { it in 0f..100f },
            description = binding.edDescription.text?.toString()?.trim(),
            dosageForm = binding.autoCompleteDosageForm.text?.toString()?.trim() ?: "",
            strengths = strengths,
            manufacturer = binding.edManufacturer.text?.toString()?.trim(),
            images = imageUrls,
            requiresPrescription = binding.cbRequiresPrescription.isChecked,
            activeIngredients = activeIngredients,
            sideEffects = binding.edSideEffects.text?.toString()?.trim(),
            storageInstructions = binding.edStorageInstructions.text?.toString()?.trim(),
            pharmacistId = pharmacistId,
            pharmacyName = pharmacyName,
            pharmacyAddress = pharmacyAddress,
            quantity = quantity,
            createdAt = System.currentTimeMillis()
        )

        db.collection("Products")
            .document(product.id)
            .set(product)
            .addOnSuccessListener {
                progressDialog.dismiss()
                showToast("Medicine added successfully")
                resetForm()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                showToast("Failed to add medicine: ${e.localizedMessage}", true)
            }
    }

    private fun resetForm() {
        with(binding) {
            listOf(
                edName, edGenericName, edDescription, edPrice,
                offerPercentage, edManufacturer, edActiveIngredients,
                edSideEffects, edStorageInstructions, edStrength, edQuantity
            ).forEach { it.text?.clear() }

            autoCompleteCategory.text?.clear()
            autoCompleteDosageForm.text?.clear()
            categoryInputLayout.error = null
            dosageFormInputLayout.error = null
            cbRequiresPrescription.isChecked = false
        }

        strengths.clear()
        activeIngredients.clear()
        selectedImages.clear()
        updateSelectedStrengths()
        updateSelectedIngredients()
        updateSelectedImages()
    }

    private fun showToast(message: String, isError: Boolean = false) {
        Toast.makeText(this, message, if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val PICK_IMAGES_REQUEST = 101
    }
}