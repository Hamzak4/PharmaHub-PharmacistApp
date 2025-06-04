package com.example.pharmacistApp.activity.pharmacist

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pharmacistApp.R
import com.example.pharmacistApp.adapters.OrderReportAdapter
import com.example.pharmacistApp.data.OrderReportItem
import com.example.pharmacistApp.databinding.ActivityReportsBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportsBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: OrderReportAdapter
    private val orderReportItems = mutableListOf<OrderReportItem>()
    private var totalOrders = 0
    private var completedOrders = 0
    private var pendingOrders = 0
    private var totalRevenue = 0.0
    private var currentPharmacistId = ""
    private var currentFilterStartDate: Date? = null
    private var currentFilterEndDate: Date? = null
    private val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        maximumFractionDigits = 2
        currency = java.util.Currency.getInstance("PKR")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentPharmacistId = auth.currentUser?.uid ?: ""

        setupToolbar()
        setupRecyclerView()
        setupDateFilters()
        setupButtons()
        fetchOrderStatistics()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Sales Reports"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = OrderReportAdapter(orderReportItems) { order ->

        }
        binding.recentOrdersRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.recentOrdersRecyclerView.adapter = adapter
    }

    private fun setupDateFilters() {
        binding.btnToday.setOnClickListener {
            filterByDate("today")
        }

        binding.btnThisWeek.setOnClickListener {
            filterByDate("week")
        }

        binding.btnThisMonth.setOnClickListener {
            filterByDate("month")
        }

        binding.btnAllTime.setOnClickListener {
            filterByDate("all")
        }

        binding.btnCustomDate.setOnClickListener {
            showDateRangePicker()
        }
    }

    private fun setupButtons() {
        binding.btnGenerateReport.setOnClickListener {
            generatePdfReport()
        }

        binding.btnShareReport.setOnClickListener {
            shareReport()
        }
    }

    private fun filterByDate(period: String) {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        val startDate: Date

        when (period) {
            "today" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startDate = calendar.time
                binding.tvDateRange.text = "Today's Report"
            }
            "week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startDate = calendar.time
                binding.tvDateRange.text = "This Week's Report"
            }
            "month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startDate = calendar.time
                binding.tvDateRange.text = "This Month's Report"
            }
            else -> {
                // For "all" or any other value, set start date to very old date
                calendar.add(Calendar.YEAR, -10)
                startDate = calendar.time
                binding.tvDateRange.text = "All Time Report"
            }
        }

        currentFilterStartDate = startDate
        currentFilterEndDate = endDate
        fetchOrderStatisticsByDateRange(startDate, endDate)
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .setSelection(
                androidx.core.util.Pair(
                    MaterialDatePicker.todayInUtcMilliseconds(),
                    MaterialDatePicker.todayInUtcMilliseconds()
                )
            )
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = Date(selection.first ?: 0)
            val endDate = Date(selection.second ?: 0)

            currentFilterStartDate = startDate
            currentFilterEndDate = endDate

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val dateRangeText = "${dateFormat.format(startDate)} - ${dateFormat.format(endDate)}"
            binding.tvDateRange.text = dateRangeText

            fetchOrderStatisticsByDateRange(startDate, endDate)
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun fetchOrderStatistics() {
        binding.progressBar.visibility = View.VISIBLE
        resetCounters()

        if (currentPharmacistId.isEmpty()) {
            Toast.makeText(this, "Pharmacist ID not found", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        firestore.collection("orders")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val filteredDocuments = querySnapshot.documents.filter { document ->
                    doesOrderBelongToPharmacist(document, currentPharmacistId)
                }
                processOrderData(filteredDocuments)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching orders: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun fetchOrderStatisticsByDateRange(startDate: Date, endDate: Date) {
        binding.progressBar.visibility = View.VISIBLE
        resetCounters()

        if (currentPharmacistId.isEmpty()) {
            Toast.makeText(this, "Pharmacist ID not found", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        firestore.collection("orders")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val filteredDocuments = querySnapshot.documents.filter { document ->
                    val createdAt = when (val timestampValue = document.get("createdAt")) {
                        is Long -> Date(timestampValue)
                        is Number -> Date(timestampValue.toLong())
                        is com.google.firebase.Timestamp -> timestampValue.toDate()
                        else -> null
                    }

                    createdAt != null &&
                            createdAt >= startDate &&
                            createdAt <= endDate &&
                            doesOrderBelongToPharmacist(document, currentPharmacistId)
                }
                processOrderData(filteredDocuments)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching orders: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun doesOrderBelongToPharmacist(
        document: com.google.firebase.firestore.DocumentSnapshot,
        pharmacistId: String
    ): Boolean {
        val products = document.get("products") as? List<Map<String, Any>> ?: emptyList()
        return products.any { product ->
            (product["pharmacistId"] as? String) == pharmacistId
        }
    }

    private fun processOrderData(documents: List<DocumentSnapshot>) {
        orderReportItems.clear()

        for (document in documents) {
            try {
                // Safely get orderId with type checking
                val orderId = when (val id = document.get("orderId")) {
                    is String -> id
                    is Number -> id.toString()
                    else -> document.id // fallback to document ID
                }

                val orderStatus = document.getString("orderStatus") ?: "PENDING"
                val date = document.getString("date") ?: ""

                val addressMap = document.get("address") as? Map<String, Any>
                val fullName = addressMap?.get("fullName") as? String ?: ""

                val products = document.get("products") as? List<Map<String, Any>> ?: emptyList()
                var hasProductsFromThisPharmacist = false
                var orderRevenue = 0.0

                for (product in products) {
                    val productPharmacistId = product["pharmacistId"] as? String ?: ""
                    if (productPharmacistId == currentPharmacistId) {
                        hasProductsFromThisPharmacist = true
                        val price = (product["totalPrice"] as? Number)?.toDouble() ?: 0.0
                        totalRevenue += price
                        orderRevenue += price
                    }
                }

                if (!hasProductsFromThisPharmacist) continue

                totalOrders++

                when (orderStatus) {
                    "DELIVERED" -> completedOrders++
                    else -> pendingOrders++
                }

                orderReportItems.add(
                    OrderReportItem(
                        orderId = orderId,
                        customerName = fullName,
                        date = date,
                        status = orderStatus,
                        total = orderRevenue
                    )
                )
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error processing order document", e)
            }
        }

        orderReportItems.sortByDescending { it.date }
        updateUI()
    }
    private fun resetCounters() {
        totalOrders = 0
        completedOrders = 0
        pendingOrders = 0
        totalRevenue = 0.0
        orderReportItems.clear()
    }

    private fun updateUI() {
        binding.tvTotalOrders.text = totalOrders.toString()
        binding.tvCompletedOrders.text = completedOrders.toString()
        binding.tvPendingOrders.text = pendingOrders.toString()
        binding.tvTotalRevenue.text = currencyFormat.format(totalRevenue)

        val completionRate = if (totalOrders > 0) {
            (completedOrders.toFloat() / totalOrders.toFloat()) * 100
        } else {
            0f
        }
        binding.tvCompletionRate.text = String.format("%.1f%%", completionRate)

        // Update chart data
        updateChartData()

        adapter.notifyDataSetChanged()
        binding.progressBar.visibility = View.GONE

        if (orderReportItems.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.recentOrdersRecyclerView.visibility = View.GONE
            binding.chartContainer.visibility = View.GONE
            binding.reportActionsContainer.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.recentOrdersRecyclerView.visibility = View.VISIBLE
            binding.chartContainer.visibility = View.VISIBLE
            binding.reportActionsContainer.visibility = View.VISIBLE
        }
    }

    private fun updateChartData() {
        // This is a placeholder for actual chart implementation
        // In a real app, you would use a chart library like MPAndroidChart
        val chartText = when {
            totalOrders == 0 -> "No data to display"
            completedOrders == 0 -> "All orders are pending"
            pendingOrders == 0 -> "All orders completed"
            else -> "Completion rate: ${String.format("%.1f%%",
                (completedOrders.toFloat() / totalOrders.toFloat()) * 100)}"
        }
        binding.tvChartPlaceholder.text = chartText
    }

    private fun generatePdfReport() {
        binding.progressBar.visibility = View.VISIBLE

        try {
            // Create a new PDF document
            val document = PdfDocument()

            // Create a page description
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

            // Start a new page
            val page = document.startPage(pageInfo)

            // Get the canvas for drawing
            val canvas = page.canvas

            // Draw report content
            drawPdfContent(document, canvas)


            // Finish the page
            document.finishPage(page)

            // Save the document
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "Pharmacy_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val file = File(downloadsDir, fileName)

            document.writeTo(FileOutputStream(file))
            document.close()

            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_LONG).show()

            // Open the PDF file
            openPdfFile(file)
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawPdfContent(document: PdfDocument, canvas: Canvas)
    {
        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
        }

        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var yPos = 50

        // Draw title
        canvas.drawText("Pharmacy Sales Report", 50f, yPos.toFloat(), titlePaint)
        yPos += 30

        // Draw date range
        val dateRange = binding.tvDateRange.text.toString()
        canvas.drawText("Date Range: $dateRange", 50f, yPos.toFloat(), paint)
        yPos += 20

        // Draw report date
        val reportDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Report Generated: $reportDate", 50f, yPos.toFloat(), paint)
        yPos += 30

        // Draw summary section
        canvas.drawText("Summary", 50f, yPos.toFloat(), headerPaint)
        yPos += 20

        canvas.drawText("Total Orders: $totalOrders", 50f, yPos.toFloat(), paint)
        yPos += 15

        canvas.drawText("Completed Orders: $completedOrders", 50f, yPos.toFloat(), paint)
        yPos += 15

        canvas.drawText("Pending Orders: $pendingOrders", 50f, yPos.toFloat(), paint)
        yPos += 15

        val completionRate = if (totalOrders > 0) {
            (completedOrders.toFloat() / totalOrders.toFloat()) * 100
        } else {
            0f
        }
        canvas.drawText("Completion Rate: ${String.format("%.1f%%", completionRate)}", 50f, yPos.toFloat(), paint)
        yPos += 15

        canvas.drawText("Total Revenue: ${currencyFormat.format(totalRevenue)}", 50f, yPos.toFloat(), paint)
        yPos += 30

        // Draw recent orders section
        canvas.drawText("Recent Orders (${orderReportItems.size})", 50f, yPos.toFloat(), headerPaint)
        yPos += 20

        // Draw table headers
        canvas.drawText("Order ID", 50f, yPos.toFloat(), paint)
        canvas.drawText("Customer", 150f, yPos.toFloat(), paint)
        canvas.drawText("Date", 300f, yPos.toFloat(), paint)
        canvas.drawText("Status", 400f, yPos.toFloat(), paint)
        canvas.drawText("Amount", 500f, yPos.toFloat(), paint)
        yPos += 15

        // Draw a line under headers
        canvas.drawLine(50f, yPos.toFloat(), 550f, yPos.toFloat(), paint)
        yPos += 10

        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var canvas = page.canvas


        // Draw order items
        for ((index, order) in orderReportItems.withIndex()) {
            // draw order data
            canvas.drawText(order.orderId.take(8), 50f, yPos.toFloat(), paint)
            canvas.drawText(order.customerName.take(15), 150f, yPos.toFloat(), paint)
            canvas.drawText(order.date.take(10), 300f, yPos.toFloat(), paint)
            canvas.drawText(order.status, 400f, yPos.toFloat(), paint)
            canvas.drawText(currencyFormat.format(order.total), 500f, yPos.toFloat(), paint)
            yPos += 15

            if (yPos > 800) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, document.pages.size + 1).create()
                page = document.startPage(newPageInfo)
                canvas = page.canvas
                yPos = 50
            }
        }
        document.finishPage(page)
    }

    private fun openPdfFile(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        startActivity(intent)
    }

    private fun shareReport() {
        // In a real implementation, you would generate a temporary PDF or CSV
        // and share it. For now, we'll share a text summary.

        val shareText = buildString {
            appendln("Pharmacy Sales Report")
            appendln("Date Range: ${binding.tvDateRange.text}")
            appendln("Generated: ${SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date())}")
            appendln()
            appendln("Summary:")
            appendln("- Total Orders: $totalOrders")
            appendln("- Completed Orders: $completedOrders")
            appendln("- Pending Orders: $pendingOrders")

            val completionRate = if (totalOrders > 0) {
                (completedOrders.toFloat() / totalOrders.toFloat()) * 100
            } else {
                0f
            }
            appendln("- Completion Rate: ${String.format("%.1f%%", completionRate)}")
            appendln("- Total Revenue: ${currencyFormat.format(totalRevenue)}")
        }

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }
}