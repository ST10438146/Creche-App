package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class StaffHomeActivity : AppCompatActivity() {

    private val TAG = "StaffHomeActivity"
    private lateinit var sharedPrefManager: SharedPrefManager
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPrefManager = SharedPrefManager.getInstance(this)

        // Try to get user from SharedPrefManager, fallback to FirebaseAuth
        val user = sharedPrefManager.getUser() ?: auth.currentUser?.let { fu ->
            User(
                uid = fu.uid,
                id = fu.uid,
                name = fu.displayName ?: fu.email ?: "Staff",
                email = fu.email ?: "",
                phone = "",
                role = UserRole.STAFF.name
            )
        }

        // Welcome toast (non-blocking)
        val userName = user?.name ?: "Staff"
        Toast.makeText(this, "Welcome, $userName", Toast.LENGTH_SHORT).show()

        // Set up bottom navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (bottomNavigation == null) {
            Log.w(TAG, "bottomNavigation view not found in activity_home.xml")
            return
        }


    }

    private fun logout() {
        try {
            sharedPrefManager.clearUser()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear SharedPref user: ${e.message}")
        }
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
