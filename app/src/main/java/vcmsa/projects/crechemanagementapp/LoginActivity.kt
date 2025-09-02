package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import vcmsa.projects.crechemanagementapp.databinding.ActivityLoginBinding
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var firestoreDb: FirebaseFirestore

    private var selectedUserType = UserRole.PARENT
    private var isPasswordVisible = false

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root) // Use binding.root for ViewBinding

        // Initialize Firebase instances
        firestoreDb = FirebaseFirestore.getInstance()

        sharedPrefManager = SharedPrefManager.getInstance(this)

        setupUserTypeSelection()
        setupBiometricAuth()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupUserTypeSelection() {
        // Set parent as default selected
        selectUserType(UserRole.PARENT)
        binding.adminCard.setOnClickListener { selectUserType(UserRole.ADMIN) }
        binding.staffCard.setOnClickListener { selectUserType(UserRole.STAFF) }
        binding.parentCard.setOnClickListener { selectUserType(UserRole.PARENT) }
    }

    private fun selectUserType(userType: UserRole) {
        selectedUserType = userType
        // Reset all cards
        binding.adminCard.isSelected = false
        binding.staffCard.isSelected = false
        binding.parentCard.isSelected = false
        // Select the chosen card
        when (userType) {
            UserRole.ADMIN -> binding.adminCard.isSelected = true
            UserRole.STAFF -> binding.staffCard.isSelected = true
            UserRole.PARENT -> binding.parentCard.isSelected = true
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

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Biometric authentication successful!",
                        Toast.LENGTH_SHORT).show()
                    val storedUser = sharedPrefManager.getUser()
                    if (storedUser != null) {
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
        binding.btnLogin.setOnClickListener { performLogin() }
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.togglePassword.setOnClickListener { togglePasswordVisibility() }
        binding.biometricIcon.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }
        binding.btnBack?.setOnClickListener {
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
        binding.etEmail.addTextChangedListener(textWatcher)
        binding.etPassword.addTextChangedListener(textWatcher)
    }

    private fun validateForm() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        binding.btnLogin.isEnabled = email.isNotEmpty() && password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.togglePassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.togglePassword.setImageResource(R.drawable.ic_visibility)
        }
        binding.etPassword.text?.let { binding.etPassword.setSelection(it.length) }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        firestoreDb.collection("users").document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val userRoleString = document.getString("role")
                                    val actualUserRole = try {
                                        UserRole.valueOf(userRoleString ?: UserRole.PARENT.name)
                                    } catch (e: IllegalArgumentException) {
                                        UserRole.PARENT
                                    }

                                    // Safely retrieve the profileImageUrl and other fields
                                    val profileImageUrl = document.getString("profileImageUrl") ?: ""
                                    val name = document.getString("name") ?: ""
                                    val phone = document.getString("phone") ?: ""


                                    val loggedInUser = User(
                                        id = firebaseUser.uid,
                                        name = name,
                                        email = firebaseUser.email ?: "",
                                        phone = phone,
                                        profileImageUrl = profileImageUrl
                                    )
                                    sharedPrefManager.saveUser(loggedInUser)
                                    Toast.makeText(baseContext, "Authentication successful.", Toast.LENGTH_SHORT).show()
                                    navigateToHome()
                                } else {
                                    Toast.makeText(baseContext, "User data not found in Firestore.", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                    showLoading(false)
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(baseContext, "Failed to get user data: ${e.message}", Toast.LENGTH_SHORT).show()
                                auth.signOut()
                                showLoading(false)
                            }
                    }
                } else {
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            }
    }

    private fun showLoading(show: Boolean) {
        binding.btnLogin.isEnabled = !show
        binding.btnLogin.text = if (show) "Logging in..." else "Log In"
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
