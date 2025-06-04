package com.example.pharmacistApp.activity.pharmacist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmacistApp.adapters.PrescriptionsAdapter
import com.bumptech.glide.Glide
import com.example.pharmacistApp.R
import com.example.pharmacistApp.data.Prescription
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class PrescriptionsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: View
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: com.example.pharmacistApp.adapters.PrescriptionsAdapter
    private val prescriptionsList = mutableListOf<Prescription>()
    private var lastPrescriptionCount = 0

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val NOTIFICATION_CHANNEL_ID = "prescription_channel"
        private const val NOTIFICATION_ID = 101
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_prescriptions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        checkAndRequestNotificationPermission()
        fetchPendingPrescriptions()
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain why you need the permission
                    showPermissionRationale()
                }
                else -> {
                    // Request the permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to alert you about new prescriptions")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Notification permission denied - you won't receive alerts",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showNewPrescriptionNotification(count: Int) {
        // Check if we have permission to show notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Don't show notification if permission not granted
                return
            }
        }

        val notificationMessage = if (count == 1) {
            "New prescription awaiting approval"
        } else {
            "$count new prescriptions awaiting approval"
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("New Prescriptions")
            .setContentText(notificationMessage)
            .setSmallIcon(R.drawable.ic_notifications)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
            Toast.makeText(this, notificationMessage, Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("PrescriptionsActivity", "Failed to show notification: ${e.message}")
        }
    }
    private fun fetchProductDetails(productIds: List<String>, callback: (List<Prescription.ProductDetail>) -> Unit) {
        val productsCollection = db.collection("Products")
        val productDetails = mutableListOf<Prescription.ProductDetail>()

        productIds.forEach { productId ->
            productsCollection.document(productId).get()
                .addOnSuccessListener { document ->
                    document?.let {
                        val product = Prescription.ProductDetail(
                            name = it.getString("name") ?: "Unknown Product",
                            dosageForm = it.getString("dosageForm") ?: "N/A",
                            strength = it.getString("strength") ?: "N/A",
                            genericName = it.getString("genericName") ?: "N/A"
                        )
                        productDetails.add(product)

                        // When we've fetched all products, invoke callback
                        if (productDetails.size == productIds.size) {
                            callback(productDetails)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("PrescriptionsActivity", "Error fetching product details", exception)
                    // Add placeholder if product fetch fails
                    productDetails.add(
                        Prescription.ProductDetail(
                            name = "Product ID: $productId",
                            dosageForm = "N/A",
                            strength = "N/A",
                            genericName = "N/A"
                        )
                    )

                    if (productDetails.size == productIds.size) {
                        callback(productDetails)
                    }
                }
        }
    }

    private fun checkForNewPrescriptions(snapshot: QuerySnapshot) {
        val currentCount = snapshot.size()
        if (currentCount > lastPrescriptionCount) {
            showNewPrescriptionNotification(currentCount - lastPrescriptionCount)
        }
        lastPrescriptionCount = currentCount
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.prescriptionsRecyclerView)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = com.example.pharmacistApp.adapters.PrescriptionsAdapter(
            prescriptionsList,
            object : com.example.pharmacistApp.adapters.PrescriptionsAdapter.OnActionClickListener {
                override fun onApproveClick(prescription: Prescription) {
                    approvePrescription(prescription)
                }
                override fun onRejectClick(prescription: Prescription) {
                    rejectPrescription(prescription)
                }
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun fetchPendingPrescriptions() {
        progressBar.visibility = View.VISIBLE
        db.collection("prescriptions")
            .whereEqualTo("approved", false)
            .whereEqualTo("rejected", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                progressBar.visibility = View.GONE

                when {
                    error != null -> handleFetchError(error)
                    snapshot != null -> {
                        handleSnapshot(snapshot)
                        checkForNewPrescriptions(snapshot)
                    }
                    else -> showEmptyState()
                }
            }
    }

    private fun handleFetchError(error: Exception) {
        if (error.message?.contains("index") == true) {
            Toast.makeText(
                this,
                "Database index is still deploying. Please wait 2-5 minutes.",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(
                this,
                "Error fetching prescriptions: ${error.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleSnapshot(snapshot: QuerySnapshot) {
        prescriptionsList.clear()
        snapshot.documents.forEach { document ->
            document.toObject(Prescription::class.java)?.let { prescription ->
                prescription.id = document.id

                // First get the product IDs
                val productIds = document.get("productIds") as? List<String> ?: emptyList()

                if (productIds.isNotEmpty()) {
                    // Fetch product details for these IDs
                    fetchProductDetails(productIds) { productDetails ->
                        prescription.productDetails = productDetails
                        prescriptionsList.add(prescription)

                        // Notify adapter after all products are fetched
                        if (prescriptionsList.size == snapshot.documents.size) {
                            adapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    // No products to fetch
                    prescriptionsList.add(prescription)
                    if (prescriptionsList.size == snapshot.documents.size) {
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }

        if (snapshot.isEmpty) {
            showEmptyState()
        }
    }


    private fun showEmptyState() {
        Toast.makeText(this, "No pending prescriptions found", Toast.LENGTH_SHORT).show()
    }

    private fun approvePrescription(prescription: Prescription) {
        progressBar.visibility = View.VISIBLE
        db.collection("prescriptions").document(prescription.id)
            .update(getApprovalUpdates())
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Prescription approved", Toast.LENGTH_SHORT).show()
                } else {
                    showUpdateError(task.exception, "approving")
                }
            }
    }

    private fun rejectPrescription(prescription: Prescription) {
        progressBar.visibility = View.VISIBLE
        db.collection("prescriptions").document(prescription.id)
            .update(getRejectionUpdates())
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Toast.makeText(this, "Prescription rejected", Toast.LENGTH_SHORT).show()
                } else {
                    showUpdateError(task.exception, "rejecting")
                }
            }
    }

    private fun getApprovalUpdates() = mapOf(
        "approved" to true,
        "pending" to false,
        "status" to "approved",
        "updatedAt" to System.currentTimeMillis()
    )

    private fun getRejectionUpdates() = mapOf(
        "rejected" to true,
        "pending" to false,
        "status" to "rejected",
        "updatedAt" to System.currentTimeMillis()
    )

    private fun showUpdateError(exception: Exception?, action: String) {
        Toast.makeText(
            this,
            "Error $action prescription: ${exception?.localizedMessage ?: "Unknown error"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    class PrescriptionsAdapter(
        private val prescriptions: List<Prescription>,
        private val onApproveClick: (Prescription) -> Unit,
        private val onRejectClick: (Prescription) -> Unit
    ) : RecyclerView.Adapter<PrescriptionsAdapter.PrescriptionViewHolder>() {

        inner class PrescriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.prescriptionImage)
            val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
            val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
            val approveButton: Button = itemView.findViewById(R.id.approveButton)
            val rejectButton: Button = itemView.findViewById(R.id.rejectButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrescriptionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_prescription, parent, false)
            return PrescriptionViewHolder(view)
        }

        override fun onBindViewHolder(holder: PrescriptionViewHolder, position: Int) {
            val prescription = prescriptions[position]

            loadPrescriptionImage(holder, prescription)
            setPrescriptionDetails(holder, prescription)
            setupActionButtons(holder, prescription)
        }

        private fun loadPrescriptionImage(holder: PrescriptionViewHolder, prescription: Prescription) {
            Glide.with(holder.itemView.context)
                .load(prescription.prescriptionImageUrl)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_error)
                .into(holder.imageView)
        }

        private fun setPrescriptionDetails(holder: PrescriptionViewHolder, prescription: Prescription) {
            holder.dateTextView.text = prescription.formattedDate
            holder.statusTextView.text = when (prescription.status) {
                "pending" -> "Pending"
                "approved" -> "Approved"
                "rejected" -> "Rejected"
                else -> prescription.status.replace("_", " ").capitalize()
            }
        }

        private fun setupActionButtons(holder: PrescriptionViewHolder, prescription: Prescription) {
            val isPending = prescription.status == "pending"

            holder.approveButton.apply {
                isEnabled = isPending
                setOnClickListener { onApproveClick(prescription) }
            }

            holder.rejectButton.apply {
                isEnabled = isPending
                setOnClickListener { onRejectClick(prescription) }
            }
        }

        override fun getItemCount() = prescriptions.size
    }
}