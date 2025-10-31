package vcmsa.projects.crechemanagementapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*



class PaymentsFragment : Fragment() {

    private lateinit var tvCurrentBalance: TextView
    private lateinit var tvNextPaymentDue: TextView
    private lateinit var btnMakePayment: Button
    private lateinit var rvPaymentHistory: RecyclerView

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private val client = OkHttpClient()
    private lateinit var paymentProgressBar: ProgressBar

    private val paymentHistoryList = mutableListOf<Payment>()
    // private lateinit var paymentAdapter: PaymentAdapter // Uncomment when PaymentAdapter is created

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        initViews(view)
        setupClickListeners()
        loadPaymentData() // This will now load from Firestore
    }

    private fun initViews(view: View) {
        tvCurrentBalance = view.findViewById(R.id.tvCurrentBalance)
        tvNextPaymentDue = view.findViewById(R.id.tvNextPaymentDue)
        btnMakePayment = view.findViewById(R.id.btnMakePayment)
        rvPaymentHistory = view.findViewById(R.id.rvPaymentHistory)
        paymentProgressBar = view.findViewById(R.id.paymentProgressBar)
    }

    private fun setupClickListeners() {
        btnMakePayment.setOnClickListener {
            val amount = "350.00"
            val itemName = "November Creche Fees"
            val itemDesc = "Monthly creche payment"
            val email = FirebaseAuth.getInstance().currentUser?.email ?: "parent@gmail.com"

            val paymentUrl = PayFastHelper.buildPayFastUrl(amount, itemName, itemDesc, email)

            val intent = Intent(requireContext(), WebViewPayActivity::class.java)
            intent.putExtra("PAYFAST_URL", paymentUrl)
            startActivity(intent)
        }

    }

    /**
     * Loads current balance and payment history from Firestore.
     */
    private fun loadPaymentData() {
        val userId = firebaseAuth.currentUser?.uid ?: run {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Listen for real-time changes to payments for the current user
        firestoreDb.collection("payments")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING) // Order by latest payment
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading payment history: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    paymentHistoryList.clear()
                    var totalOutstanding = 0.0
                    var nextDueDate: Long? = null

                    for (doc in snapshots.documents) {
                        val payment = doc.toObject(Payment::class.java)
                        payment?.let {
                            paymentHistoryList.add(it)
                            if (it.status == "Pending") {
                                totalOutstanding += it.amount
                                if (nextDueDate == null || it.date < nextDueDate!!) {
                                    nextDueDate = it.date
                                }
                            }
                        }
                    }

                    // Update UI with calculated balance and due date
                    tvCurrentBalance.text = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(totalOutstanding)
                    if (nextDueDate != null) {
                        tvNextPaymentDue.text = "Due: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(nextDueDate!!))}"
                    } else {
                        tvNextPaymentDue.text = "No upcoming payments due."
                    }

                    // Update RecyclerView
                    // paymentAdapter.notifyDataSetChanged() // Uncomment when adapter is set up
                    setupPaymentHistory() // Re-setup adapter to ensure it reflects changes (or just notify if already set)
                }
            }
    }

    private fun setupPaymentHistory() {
        rvPaymentHistory.layoutManager = LinearLayoutManager(context)
        // paymentAdapter = PaymentAdapter(paymentHistoryList) // Uncomment when PaymentAdapter is created
        // rvPaymentHistory.adapter = paymentAdapter // Uncomment when PaymentAdapter is created
    }

    /**
     * Simulates making a payment and records it in Firestore.
     * In a real app, this would integrate with a payment gateway (e.g., Stripe, PayPal).
     */
    private fun showPaymentDialog() {
        val userId = firebaseAuth.currentUser?.uid ?: run {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Amount, description — in real UI ask user or compute from DB
        val amount = 250.00
        val name = "Parent Name" // get from SharedPrefManager.getUser()
        val email = firebaseAuth.currentUser?.email ?: ""

        // Call your server to create signed payload
        val json = JSONObject()
        json.put("userId", userId)
        json.put("amount", amount)
        json.put("name", name)
        json.put("email", email)

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            json.toString()
        )

        // Your server endpoint that returns { payfastUrl, payload, signature }
        val request = Request.Builder()
            .url("https://crecheconnectpayfastserver20251030090609-g5haf5fha0brhaa9.southafricanorth-01.azurewebsites.net")
            .post(requestBody)
            .build()

        paymentProgressBar.visibility = View.VISIBLE

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    paymentProgressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to contact payment server: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Payment server error: ${response.message}", Toast.LENGTH_LONG).show()
                        }
                        return
                    }
                    val respText = it.body.string()
                    val obj = JSONObject(respText)
                    val payfastUrl = obj.getString("payfastUrl")
                    val payload = obj.getJSONObject("payload")
                    val signature = obj.getString("signature")

                    // Build an auto-posting HTML form with payload fields and signature
                    val htmlForm = buildAutoPostForm(payfastUrl, payload, signature)

                    // Launch WebView activity
                    activity?.runOnUiThread {
                        val intent = Intent(requireContext(), WebViewPayActivity::class.java)
                        intent.putExtra("PAYFAST_URL", payfastUrl)
                        startActivityForResult(intent, REQUEST_PAYFAST)
                    }

                }
            }
        })
    }
    /**
     * Marks the most recent pending payment as 'Paid' in Firestore.
     */
    private fun updateFirestorePaymentStatus() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        firestoreDb.collection("payments")
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "Pending")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(context, "No pending payments found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val doc = snapshot.documents.first()
                firestoreDb.collection("payments")
                    .document(doc.id)
                    .update("status", "Paid")
                    .addOnSuccessListener {
                        Toast.makeText(context, "Payment updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update payment: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error finding payment: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun buildAutoPostForm(actionUrl: String, payload: JSONObject, signature: String): String {
        // Build hidden inputs from payload and add the signature field
        val sb = StringBuilder()
        sb.append("<html><head><meta name='viewport' content='width=device-width, initial-scale=1' /></head><body>")
        sb.append("<form id='payform' method='post' action='").append(actionUrl).append("'>")

        val keys = payload.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = payload.optString(k)
            sb.append("<input type='hidden' name='").append(escapeHtml(k)).append("' value='").append(escapeHtml(v)).append("' />")
        }
        // signature field expected by PayFast is 'signature'
        sb.append("<input type='hidden' name='signature' value='").append(escapeHtml(signature)).append("' />")

        sb.append("</form>")
        sb.append("<script>document.getElementById('payform').submit();</script>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun escapeHtml(s: String): String {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;").replace("\"","&quot;")
    }

    companion object {
        const val REQUEST_PAYFAST = 777
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PAYFAST) {
            val paymentStatus = data?.getStringExtra("payment_status") ?: "unknown"
            if (paymentStatus == "success") {
                updateFirestorePaymentStatus()
            } else {
                Toast.makeText(context, "Payment not completed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}