package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import android.text.InputType
import android.widget.*
import com.google.firebase.auth.FirebaseAuth // Import Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson // Import Gson


class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tvTimer: TextView
    private lateinit var togglePassword: ImageView
    private lateinit var biometricIcon: ImageView
    private lateinit var adminCard: View
    private lateinit var staffCard: View
    private lateinit var parentCard: View

    private lateinit var sharedPrefManager: SharedPrefManager
    private var selectedUserType = UserRole.PARENT // Default selected user type
    private var isPasswordVisible = false

    // Firebase instances
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    // Biometric authentication
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        sharedPrefManager = SharedPrefManager.getInstance(this) // Initialize here

        initViews()
        setupUserTypeSelection()
        setupBiometricAuth()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tvTimer = findViewById(R.id.tvTimer)
        togglePassword = findViewById(R.id.togglePassword)
        biometricIcon = findViewById(R.id.biometricIcon)
        adminCard = findViewById(R.id.adminCard)
        staffCard = findViewById(R.id.staffCard)
        parentCard = findViewById(R.id.parentCard)
    }

    private fun setupUserTypeSelection() {
        // Set parent as default selected
        selectUserType(UserRole.PARENT)
        adminCard.setOnClickListener { selectUserType(UserRole.ADMIN) }
        staffCard.setOnClickListener { selectUserType(UserRole.STAFF) }
        parentCard.setOnClickListener { selectUserType(UserRole.PARENT) }
    }

    private fun selectUserType(userType: UserRole) {
        selectedUserType = userType
        // Reset all cards
        adminCard.isSelected = false
        staffCard.isSelected = false
        parentCard.isSelected = false
        // Select the chosen card
        when (userType) {
            UserRole.ADMIN -> adminCard.isSelected = true
            UserRole.STAFF -> staffCard.isSelected = true
            UserRole.PARENT -> parentCard.isSelected = true
        }
    }

    private fun setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this as FragmentActivity,
            executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString",
                        Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result:
                                                       BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Biometric authentication successful!",
                        Toast.LENGTH_SHORT).show()
                    // In a production app, you'd prompt for password if biometric fails,
                    // or use a secure credential manager to store Firebase tokens.
                    // For this beginner solution, we'll navigate directly to home.
                    // If you have a mechanism to store credentials securely after first login,
                    // you would use them here to re-authenticate with Firebase.
                    val storedUser = sharedPrefManager.getUser()
                    if (storedUser != null) {
                        // Re-authenticate with Firebase using stored credentials if possible
                        // (advanced topic not covered here directly).
                        // For now, if a user is stored, assume "logged in" via biometric means
                        // and navigate.
                        navigateToHome()
                    } else {
                        Toast.makeText(applicationContext, "No stored user data for biometric login. Please log in with email/password first.", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Biometric authentication failed",
                        Toast.LENGTH_SHORT).show()
                }
            })
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Crèche App")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener { performLogin() }
        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        togglePassword.setOnClickListener { togglePasswordVisibility() }
        biometricIcon.setOnClickListener {
            // Only authenticate if email/password fields are empty, or if we are using a
            // stored biometric token (which is beyond this beginner scope).
            // For this demo, biometric will just trigger navigation to Home.
            biometricPrompt.authenticate(promptInfo)
        }
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
            finish()
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }
        etEmail.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)
    }

    private fun validateForm() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePassword.setImageResource(R.drawable.ic_visibility)
        }
        etPassword.setSelection(etPassword.text.length)
    }

    /**
     * Performs user login using Firebase Authentication.
     */
    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.",
                Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true) // Show loading indicator

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = firebaseAuth.currentUser
                    user?.let { firebaseUser ->
                        // Fetch user role from Firestore
                        firestoreDb.collection("users")
                            .document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    val userRoleString = document.getString("role")
                                    val actualUserRole = try {
                                        UserRole.valueOf(userRoleString ?: UserRole.PARENT.name)
                                    } catch (e: IllegalArgumentException) {
                                        UserRole.PARENT // Default if role is invalid
                                    }

                                    // Create a local User object
                                    val loggedInUser = User(
                                        id = firebaseUser.uid,
                                        name = document.getString("name") ?: "",
                                        email = firebaseUser.email ?: "",
                                        phone = document.getString("phone") ?: "",
                                        role = actualUserRole,
                                        profileImageUrl = document.getString("profileImageUrl") ?: ""
                                    )
                                    // Save the logged-in user to SharedPrefManager
                                    sharedPrefManager.saveUser(loggedInUser)

                                    Toast.makeText(baseContext, "Authentication successful.",
                                        Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                } else {
                                    // User document not found in Firestore after successful auth
                                    Toast.makeText(baseContext, "User data not found in Firestore.",
                                        Toast.LENGTH_SHORT).show()
                                    firebaseAuth.signOut() // Sign out user if data is missing
                                    showLoading(false)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(baseContext, "Failed to get user data: ${e.message}",
                                    Toast.LENGTH_SHORT).show()
                                firebaseAuth.signOut() // Sign out on Firestore error
                                showLoading(false)
                            }
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
    }

    private fun showLoading(show: Boolean) {
        btnLogin.isEnabled = !show
        btnLogin.text = if (show) "Logging in..." else "Log In"
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish() // Finish LoginActivity
    }
}
