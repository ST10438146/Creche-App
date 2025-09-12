package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class ParentHomeActivity : AppCompatActivity() {

    private val TAG = "ParentHomeActivity"
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
                name = fu.displayName ?: fu.email ?: "Parent",
                email = fu.email ?: "",
                phone = "",
                role = UserRole.PARENT.name
            )
        }

        // Welcome toast (non-blocking)
        val userName = user?.name ?: "Parent"
        Toast.makeText(this, "Welcome, $userName", Toast.LENGTH_SHORT).show()

        // Set up bottom navigation
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        if (bottomNavigation == null) {
            Log.w(TAG, "bottomNavigation view not found in activity_home.xml")
            return
        }

       }
    }


