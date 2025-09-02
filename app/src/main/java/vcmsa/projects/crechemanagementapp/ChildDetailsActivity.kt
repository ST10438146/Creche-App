package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vcmsa.projects.crechemanagementapp.databinding.ActivityChildDetailsBinding

class ChildDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildDetailsBinding
    private val db = Firebase.firestore
    private lateinit var firestoreDb: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestoreDb = FirebaseFirestore.getInstance()
        val childId = intent.getStringExtra("CHILD_ID")

        if (childId != null) {
            fetchChildDetails(childId)
        } else {
            Toast.makeText(this, "Child ID not provided.", Toast.LENGTH_SHORT).show()
            finish() // Close the activity if no ID is passed
        }

    }

    private fun fetchChildDetails(childId: String) {
        db.collection("children").document(childId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Assuming child document has fields like 'name', 'age', 'attendance'
                    val name = document.getString("name")
                    val age = document.getLong("age")?.toString()
                    val attendance = document.getString("attendance")
                    val profileImageUrl = document.getString("profileImageUrl") ?: ""

                    binding.childNameTextView.text = name ?: "Name not found"
                    binding.childAgeTextView.text = "Age: ${age ?: "N/A"}"
                    binding.childAttendanceTextView.text = "Today's Attendance: ${attendance ?: "N/A"}"
                } else {
                    Toast.makeText(this, "Child details not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load child details.", Toast.LENGTH_SHORT).show()
                Log.e("ChildDetails", "Error fetching child document", e)
            }
    }
}