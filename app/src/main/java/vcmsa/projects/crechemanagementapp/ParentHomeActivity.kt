package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vcmsa.projects.crechemanagementapp.databinding.ActivityParentHomeBinding

class ParentHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentHomeBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.viewChildDetailsButton.setOnClickListener {
            val parentUid = auth.currentUser?.uid
            if (parentUid != null) {
                db.collection("users").document(parentUid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val childId = document.getString("childId")
                            if (childId != null) {
                                val intent = Intent(this, ChildDetailsActivity::class.java)
                                intent.putExtra("CHILD_ID", childId)
                                startActivity(intent)
                            } else {
                                Toast.makeText(this, "Child ID not found.", Toast.LENGTH_SHORT).show()
                                Log.w("ParentHome", "Child ID not found for parent: $parentUid")
                            }
                        } else {
                            Toast.makeText(this, "User document not found.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to get user data.", Toast.LENGTH_SHORT).show()
                        Log.e("ParentHome", "Error fetching user document", e)
                    }
            } else {
                Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}