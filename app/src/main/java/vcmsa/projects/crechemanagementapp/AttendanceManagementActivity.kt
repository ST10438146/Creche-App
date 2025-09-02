package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AttendanceManagementActivity : AppCompatActivity() {

    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var attendanceAdapter: AttendanceAdapter
    private val attendanceRecords = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_management)

        // Initialize Firebase
        firestoreDb = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        recyclerView = findViewById(R.id.attendanceRecyclerView)
        progressBar = findViewById(R.id.attendanceProgressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load data
        fetchUsersForAttendance()

        // Setup save button
        findViewById<View>(R.id.saveAttendanceButton).setOnClickListener {
            saveAttendanceRecords()
        }

        // Setup back button
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }

    private fun fetchUsersForAttendance() {
        progressBar.visibility = View.VISIBLE
        firestoreDb.collection("users")
            .whereIn("role", listOf(UserRole.PARENT.name, UserRole.STAFF.name))
            .get()
            .addOnSuccessListener { result ->
                progressBar.visibility = View.GONE
                if (result.isEmpty) {
                    tvEmptyState.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val users = result.toObjects(User::class.java).filterNotNull()
                recyclerView.adapter = attendanceAdapter
                tvEmptyState.visibility = View.GONE
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                Log.e("Attendance", "Error getting users: ", exception)
                Toast.makeText(this, "Failed to load users.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAttendanceRecords() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val attendanceCollection = firestoreDb.collection("attendance")

        attendanceRecords.forEach { (userId, isPresent) ->
            val attendanceData = hashMapOf(
                "userId" to userId,
                "date" to currentDate,
                "isPresent" to isPresent,
                "recordedBy" to (auth.currentUser?.uid ?: "unknown")
            )

            // Use a specific document ID to prevent duplicate records for the same user on the same day
            val docId = "${userId}_$currentDate"
            attendanceCollection.document(docId)
                .set(attendanceData)
                .addOnSuccessListener {
                    Log.d("Attendance", "Attendance for $userId on $currentDate saved successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("Attendance", "Error saving attendance for $userId.", e)
                    Toast.makeText(this, "Error saving some attendance records.", Toast.LENGTH_SHORT).show()
                }
        }
        Toast.makeText(this, "Attendance records saved!", Toast.LENGTH_SHORT).show()
        finish()
    }
}