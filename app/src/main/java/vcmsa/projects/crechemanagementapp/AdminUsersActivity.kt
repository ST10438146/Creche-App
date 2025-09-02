package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.crechemanagementapp.databinding.ActivityAdminUsersBinding


class AdminUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminUsersBinding
    private lateinit var usersAdapter: UsersAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchUsers()
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter(this, mutableListOf()) { user, newRole ->
            updateUserRole(user, newRole)
        }
        binding.usersRecyclerView.adapter = usersAdapter
        binding.usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminUsersActivity)
            adapter = usersAdapter
        }
    }

    private fun fetchUsers() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        firestore.collection("users")
            .get()
            .addOnSuccessListener { result ->
                binding.loadingProgressBar.visibility = View.GONE
                val userList = result.map { doc ->
                    doc.toObject(User::class.java)
                }
                usersAdapter.updateUsers(userList)
            }
            .addOnFailureListener { e ->
                binding.loadingProgressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserRole(user: User, newRole: UserRole) {
        // Find the user document in Firestore and update the 'role' field
        firestore.collection("users").document(user.uid)
            .update("role", newRole.name)
            .addOnSuccessListener {
                Toast.makeText(this, "Role updated to ${newRole.name} for ${user.name}", Toast.LENGTH_SHORT).show()
                // The guide suggests using Cloud Functions to set custom claims.
                // In this client-side example, we rely on the Firestore rule
                // and the user's token refreshing for the change to take effect.
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update role: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
