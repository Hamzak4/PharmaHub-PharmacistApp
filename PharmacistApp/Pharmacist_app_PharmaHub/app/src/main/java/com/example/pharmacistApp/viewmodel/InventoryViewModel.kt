package com.example.pharmacistApp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pharmacistApp.data.Product
import com.example.pharmacistApp.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _medicines = MutableLiveData<Resource<List<Product>>>()
    val medicines: LiveData<Resource<List<Product>>> = _medicines

    fun loadMedicines(pharmacistId: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val medicinesList = firestore.collection("Products")
                    .whereEqualTo("pharmacistId", pharmacistId)
                    .get()
                    .await()
                    .toObjects(Product::class.java)

                // Ensure inStock field aligns with quantity
                medicinesList.forEach { product ->
                    product.inStock = product.quantity > 0
                }

                val sortedList = medicinesList.sortedByDescending { it.createdAt }
                _medicines.postValue(Resource.Success(sortedList))

                // Log success for debugging
                Log.d("InventoryViewModel", "Loaded ${sortedList.size} medicines")
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error loading medicines", e)
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }
    fun searchMedicines(query: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get all medicines first, then filter by query
                // This is because Firestore doesn't support complex text search
                val medicinesList = firestore.collection("Products")
                    .get()
                    .await()
                    .toObjects(Product::class.java)
                    .filter {
                        it.name.contains(query, ignoreCase = true) ||
                                it.genericName.contains(query, ignoreCase = true) ||
                                it.activeIngredients.any { ingredient ->
                                    ingredient.contains(query, ignoreCase = true)
                                }
                    }
                    .sortedByDescending { it.createdAt }

                _medicines.postValue(Resource.Success(medicinesList))
            } catch (e: Exception) {
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun filterByLowStock(pharmacistId: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val medicinesList = firestore.collection("Products")
                    .whereEqualTo("pharmacistId", pharmacistId)
                    .get()
                    .await()
                    .toObjects(Product::class.java)
                    .filter { it.quantity > 0 && it.quantity <= 10 }
                    .sortedBy { it.quantity }

                _medicines.postValue(Resource.Success(medicinesList))
            } catch (e: Exception) {
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    // Update filterByOutOfStock function
    fun filterByOutOfStock(pharmacistId: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val medicinesList = firestore.collection("Products")
                    .whereEqualTo("pharmacistId", pharmacistId)
                    .get()
                    .await()
                    .toObjects(Product::class.java)
                    .filter { it.quantity <= 0 } // Simplified check based on quantity only

                // Update inStock field to match quantity status
                medicinesList.forEach { product ->
                    product.inStock = product.quantity > 0
                }

                _medicines.postValue(Resource.Success(medicinesList))
                Log.d("InventoryViewModel", "Found ${medicinesList.size} out of stock medicines")
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error filtering out of stock", e)
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun filterByCategory(pharmacistId: String, category: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val medicinesList = firestore.collection("Products")
                    .whereEqualTo("pharmacistId", pharmacistId)
                    .whereEqualTo("category", category)
                    .get()
                    .await()
                    .toObjects(Product::class.java)
                    .sortedByDescending { it.createdAt }

                _medicines.postValue(Resource.Success(medicinesList))
            } catch (e: Exception) {
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun filterByPrescriptionRequired(pharmacistId: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val medicinesList = firestore.collection("Products")
                    .whereEqualTo("pharmacistId", pharmacistId)
                    .whereEqualTo("requiresPrescription", true)
                    .get()
                    .await()
                    .toObjects(Product::class.java)
                    .sortedByDescending { it.createdAt }

                _medicines.postValue(Resource.Success(medicinesList))
            } catch (e: Exception) {
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun filterByRecentlyAdded(pharmacistId: String) {
        _medicines.value = Resource.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get medicines added in the last 7 days
                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                val medicinesList = firestore.collection("Products")
                    .whereEqualTo("pharmacistId", pharmacistId)
                    .whereGreaterThan("createdAt", oneWeekAgo)
                    .get()
                    .await()
                    .toObjects(Product::class.java)
                    .sortedByDescending { it.createdAt }

                _medicines.postValue(Resource.Success(medicinesList))
            } catch (e: Exception) {
                _medicines.postValue(Resource.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    fun deleteMedicine(productId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("Products")
                    .document(productId)
                    .delete()
                    .await()

                // Reload medicines instead of manipulating the current list
                // This ensures sync with database
                val currentPharmacistId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentPharmacistId != null) {
                    loadMedicines(currentPharmacistId)
                } else {
                    // Just update the list if we can't reload
                    val currentList = _medicines.value?.data
                    if (currentList != null) {
                        val updatedList = currentList.filter { it.id != productId }
                        _medicines.postValue(Resource.Success(updatedList))
                    }
                }
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error deleting medicine", e)
                // Post error to UI
                _medicines.postValue(Resource.Error("Failed to delete medicine: ${e.message}"))
            }
        }
    }
}