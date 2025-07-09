package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    }

    private fun setupClickListeners() {
        btnMakePayment.setOnClickListener {
            showPaymentDialog() // This will now interact with Firestore to record payment
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

        // This is a simplified mock payment. In a real app, you'd have
        // a complex flow with payment gateway integration.
        val mockAmount = 250.00
        val mockDescription = "Payment for Activity Fee"
        val paymentStatus = "Paid" // Assuming immediate success for mock

        val newPayment = Payment(
            id = firestoreDb.collection("payments").document().id,
            userId = userId,
            amount = mockAmount,
            description = mockDescription,
            date = System.currentTimeMillis(),
            status = paymentStatus
        )

        firestoreDb.collection("payments")
            .document(newPayment.id)
            .set(newPayment)
            .addOnSuccessListener {
                Toast.makeText(context, "Payment of ${NumberFormat.getCurrencyInstance(Locale("en", "ZA")).format(mockAmount)} successful!", Toast.LENGTH_SHORT).show()
                // Data will automatically update via addSnapshotListener in loadPaymentData()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Payment failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}