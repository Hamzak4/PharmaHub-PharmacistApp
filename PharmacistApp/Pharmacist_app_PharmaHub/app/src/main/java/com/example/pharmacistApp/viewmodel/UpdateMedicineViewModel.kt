package com.example.pharmacistApp.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.cloudinary.CloudinaryHelper
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class UpdateMedicineViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _productDetails = MutableLiveData<Resource<Product>>()
    val productDetails: LiveData<Resource<Product>> = _productDetails

    private val _updateStatus = MutableLiveData<Resource<Boolean>>()
    val updateStatus: LiveData<Resource<Boolean>> = _updateStatus

    fun loadProductDetails(productId: String) {
        _productDetails.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Change "products" to "Products" to match collection name in Firestore
                val document = firestore.collection("Products")
                    .document(productId)
                    .get()
                    .await()

                if (document.exists()) {
                    val product = document.toObject(Product::class.java)?.copy(id = document.id)
                    if (product != null) {
                        _productDetails.postValue(Resource.Success(product))
                    } else {
                        _productDetails.postValue(Resource.Error("Failed to parse product data"))
                    }
                } else {
                    _productDetails.postValue(Resource.Error("Product not found"))
                }
            } catch (e: Exception) {
                _productDetails.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun updateProduct(updatedProduct: Product, newImageUri: Uri?) {
        _updateStatus.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Handle image upload first if there's a new image
                val finalProduct = if (newImageUri != null) {
                    val imageUrl = uploadImageToCloudinary(newImageUri)

                    // Create a new images list with the new image as the first item
                    val currentImages = updatedProduct.images.toMutableList()
                    if (currentImages.isEmpty()) {
                        updatedProduct.copy(images = listOf(imageUrl))
                    } else {
                        // Replace the first image with the new one
                        currentImages[0] = imageUrl
                        updatedProduct.copy(images = currentImages)
                    }
                } else {
                    updatedProduct
                }

                // Update product in Firestore
                firestore.collection("Products")
                    .document(finalProduct.id)
                    .set(finalProduct)
                    .await()

                _updateStatus.postValue(Resource.Success(true))
            } catch (e: Exception) {
                _updateStatus.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            CloudinaryHelper.uploadImage(
                imageUri,
                onSuccess = { imageUrl ->
                    continuation.resume(imageUrl)
                },
                onError = { error ->
                    continuation.resumeWithException(Exception("Failed to upload image: $error"))
                }
            )
        }
    }
}