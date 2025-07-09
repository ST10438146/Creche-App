package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var firebaseAuth: FirebaseAuth // Declare Firebase Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SharedPrefManager
        sharedPrefManager = SharedPrefManager.getInstance(this)
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Set up UI
        setupUI()

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 2000) // 2 second delay
    }

    private fun setupUI() {
         // Handle learn more button
        findViewById<View>(R.id.btnLearnMore)?.setOnClickListener {
            // Show app intro or tutorial
            navigateToLogin() // Directs to login as a starting point for "Learn More"
        }
    }

    private fun toggleDarkMode() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun navigateToNextScreen() {
        // Check if a user is currently logged in with Firebase Auth
        if (firebaseAuth.currentUser != null) {
            // User is already logged in, go to home
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // No user logged in, navigate to login
            navigateToLogin()
        }
        finish() // Finish MainActivity so user can't navigate back to splash
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish() // Finish MainActivity
    }
}