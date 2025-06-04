package com.example.pharmacistApp.activity.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.ComplaintsAdapter
import com.example.pharmacistApp.data.Complaint
import com.example.pharmacistApp.data.ComplaintResponse
import com.example.pharmacistApp.databinding.ActivityComplaintsManagementBinding
import com.example.pharmacistApp.utils.Resource
import com.example.pharmacistApp.viewmodel.ComplaintsViewModel
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ComplaintsActivity"

@AndroidEntryPoint
class ComplaintsManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComplaintsManagementBinding
    private val viewModel: ComplaintsViewModel by viewModels()
    private lateinit var complaintsAdapter: ComplaintsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityComplaintsManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        // Check if we're viewing a specific complaint
        val complaintId = intent.getStringExtra("COMPLAINT_ID")
        if (complaintId != null) {
            viewModel.fetchComplaintById(complaintId)
            supportActionBar?.title = "Complaint Details"
        }
        // Check if we should show user-specific complaints
        else {
            val userId = intent.getStringExtra("USER_ID")
            if (userId != null) {
                viewModel.fetchComplaintsByUserId(userId)
                supportActionBar?.title = "User Complaints"
            } else {
                viewModel.fetchPendingComplaints()
                supportActionBar?.title = "All Pending Complaints"
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val isAdminView = intent.getStringExtra("COMPLAINT_ID") == null
        complaintsAdapter = ComplaintsAdapter(
            emptyList(),
            { complaint -> showResponseDialog(complaint) },
            isAdminView,
            { complaint -> showResponses(complaint) }
        )

        binding.complaintsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ComplaintsManagementActivity)
            adapter = complaintsAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@ComplaintsManagementActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.complaintsState.collect { resource ->
                    when (resource) {
                        is Resource.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.emptyStateLayout.isVisible = false
                        }
                        is Resource.Success -> {
                            binding.progressBar.isVisible = false
                            resource.data?.let { complaints ->
                                complaintsAdapter.updateList(complaints)
                                binding.emptyStateLayout.isVisible = complaints.isEmpty()

                                // For single complaint view, show responses if available
                                if (complaints.size == 1 && complaints[0].responses.isNotEmpty()) {
                                    Log.d(TAG, "Single complaint with ${complaints[0].responses.size} responses")
                                    showResponses(complaints[0])
                                }
                            }
                        }
                        is Resource.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(
                                this@ComplaintsManagementActivity,
                                resource.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun showResponseDialog(complaint: Complaint) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_response, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Respond to Complaint")
            .create()

        val etResponse = dialogView.findViewById<EditText>(R.id.etResponse)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        btnSend.setOnClickListener {
            val response = etResponse.text.toString().trim()
            if (response.isBlank()) {
                etResponse.error = "Please enter a response"
                return@setOnClickListener
            }

            // Show loading indicator
            btnSend.isEnabled = false
            btnSend.text = "Sending..."

            // Add response to Firestore
            viewModel.addComplaintResponse(complaint.id, response) { success ->
                if (success) {
                    Toast.makeText(this, "Response added successfully", Toast.LENGTH_SHORT).show()
                    // Send email with the response
                    sendEmailResponse(complaint, response)
                    dialog.dismiss()
                } else {
                    // Reset button state
                    btnSend.isEnabled = true
                    btnSend.text = "Send Response"
                    Toast.makeText(this, "Failed to add response. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showResponses(complaint: Complaint) {
        val responses = complaint.responses
        if (responses.isEmpty()) {
            Log.d(TAG, "No responses available for complaint ${complaint.id}")
            Toast.makeText(this, "No responses available", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Showing ${responses.size} responses for complaint ${complaint.id}")

        // Use the dialog layout you provided
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_responses_history, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Response History")
            .setPositiveButton("Close", null)
            .create()

        val responsesContainer = dialogView.findViewById<LinearLayout>(R.id.responsesContainer)

        // Verify we found the container
        if (responsesContainer == null) {
            Log.e(TAG, "responsesContainer not found in the layout!")
            Toast.makeText(this, "Error displaying responses", Toast.LENGTH_SHORT).show()
            return
        }

        responsesContainer.removeAllViews() // Clear previous views if any

        // Debug log to confirm responses data
        for (i in responses.indices) {
            val response = responses[i]
            Log.d(TAG, "Response $i: ${response.adminEmail}, text: ${response.response}, timestamp: ${response.timestamp}")
        }

        responses.forEach { response ->
            try {
                val responseView = LayoutInflater.from(this)
                    .inflate(R.layout.item_response, responsesContainer, false)

                val tvAdminEmail = responseView.findViewById<TextView>(R.id.tvAdminEmail)
                val tvResponse = responseView.findViewById<TextView>(R.id.tvResponse)
                val tvDate = responseView.findViewById<TextView>(R.id.tvDate)

                // Check if all views were found
                if (tvAdminEmail == null || tvResponse == null || tvDate == null) {
                    Log.e(TAG, "One or more TextView not found in item_response layout")
                    return@forEach
                }

                tvAdminEmail.text = response.adminEmail ?: "Admin"
                tvResponse.text = response.response

                // Format timestamp properly, handling different types
                val date = when (val timestamp = response.timestamp) {
                    is Timestamp -> timestamp.toDate()
                    is Date -> timestamp
                    is Long -> Date(timestamp)
                    else -> Date() // Default to current date if unknown format
                }

                tvDate.text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(date)

                // Add the view to the container
                responsesContainer.addView(responseView)
                Log.d(TAG, "Added response view to container")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding response view", e)
            }
        }

        dialog.show()
    }

    private fun sendEmailResponse(complaint: Complaint, response: String) {
        try {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(complaint.userEmail))
                putExtra(Intent.EXTRA_SUBJECT, "Response to your complaint - ${complaint.id}")
                putExtra(Intent.EXTRA_TEXT, """
                    Dear ${complaint.userName},
                    
                    Regarding your complaint:
                    "${complaint.text}"
                    
                    Our response:
                    $response
                    
                    If you have any further questions, please reply to this email or submit another complaint through our app.
                    
                    Best regards,
                    Pharmacy Support Team
                """.trimIndent())
            }

            // Check if there are email apps installed
            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, "Send email via..."))
            } else {
                Toast.makeText(this, "No email app found on your device", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send email: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}