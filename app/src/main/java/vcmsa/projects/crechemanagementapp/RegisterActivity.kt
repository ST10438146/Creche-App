package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import com.google.firebase.auth.FirebaseAuth // Import Firebase Auth
import com.google.firebase.firestore.FirebaseFirestore // Import Firebase Firestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var tvStepProgress: TextView
    private lateinit var llParentInfo: LinearLayout
    private lateinit var llChildInfo: LinearLayout
    private lateinit var llEmergencyInfo: LinearLayout
    private lateinit var llDocuments: LinearLayout
    private lateinit var llReview: LinearLayout
    private lateinit var btnNext: Button
    private lateinit var btnBack: ImageView

    // Form fields for Parent
    private lateinit var etParentName: EditText
    private lateinit var etParentEmail: EditText
    private lateinit var etParentPhone: EditText
    // Form fields for Child
    private lateinit var etChildName: EditText
    private lateinit var etChildAge: EditText // Note: In a real app, consider a DatePicker for Date of Birth
    private lateinit var etAllergies: EditText
    // Form fields for Emergency
    private lateinit var etEmergencyContact: EditText
    // New fields for registration - password
    private lateinit var etRegisterPassword: EditText // Added for new user registration

    private var currentStep = 1
    private val totalSteps = 5
    private lateinit var sharedPrefManager: SharedPrefManager

    // Firebase instances
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        initViews()
        setupClickListeners()
        setupTextWatchers() // Call text watchers setup early for all fields
        showCurrentStep()

        sharedPrefManager = SharedPrefManager.getInstance(this)
    }

    private fun initViews() {
        tvStepProgress = findViewById(R.id.tvStepProgress)
        llParentInfo = findViewById(R.id.llParentInfo)
        llChildInfo = findViewById(R.id.llChildInfo)
        llEmergencyInfo = findViewById(R.id.llEmergencyInfo)
        llDocuments = findViewById(R.id.llDocuments)
        llReview = findViewById(R.id.llReview)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)

        // Parent form fields
        etParentName = findViewById(R.id.etParentName)
        etParentEmail = findViewById(R.id.etParentEmail)
        etParentPhone = findViewById(R.id.etParentPhone)
        etRegisterPassword = findViewById(R.id.etRegisterPassword) // New: Add password field to parent info layout

        // Child form fields
        etChildName = findViewById(R.id.etChildName)
        etChildAge = findViewById(R.id.etChildAge)
        etAllergies = findViewById(R.id.etAllergies) // Already exists

        // Emergency form fields
        etEmergencyContact = findViewById(R.id.etEmergencyContact)
    }

    private fun setupClickListeners() {
        btnNext.setOnClickListener { handleNextButton() }
        btnBack.setOnClickListener { handleBackButton() }

        // Note: Upload buttons on Step 4 (Documents) would need actual file picker logic
        findViewById<Button>(R.id.btnUploadBirthCertificate)?.setOnClickListener {
            Toast.makeText(this, "Upload Birth Certificate (Not implemented yet)", Toast.LENGTH_SHORT).show()
            // Implement file picking and Firebase Storage upload here
        }
        findViewById<Button>(R.id.btnUploadMedicalRecords)?.setOnClickListener {
            Toast.makeText(this, "Upload Medical Records (Not implemented yet)", Toast.LENGTH_SHORT).show()
            // Implement file picking and Firebase Storage upload here
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateNextButton() // Call updateNextButton whenever text changes
            }
        }

        // Apply to all relevant input fields for real-time validation
        etParentName.addTextChangedListener(textWatcher)
        etParentEmail.addTextChangedListener(textWatcher)
        etParentPhone.addTextChangedListener(textWatcher)
        etRegisterPassword.addTextChangedListener(textWatcher) // New field
        etChildName.addTextChangedListener(textWatcher)
        etChildAge.addTextChangedListener(textWatcher)
        etEmergencyContact.addTextChangedListener(textWatcher)
    }

    private fun handleNextButton() {
        if (validateCurrentStep()) {
            if (currentStep < totalSteps) {
                currentStep++
                showCurrentStep()
            } else {
                // Final step - complete registration
                completeRegistration()
            }
        }
    }

    private fun handleBackButton() {
        if (currentStep > 1) {
            currentStep--
            showCurrentStep()
        } else {
            finish() // Close activity if on first step
        }
    }

    private fun showCurrentStep() {
        tvStepProgress.text = "Step $currentStep of $totalSteps"

        // Hide all steps
        llParentInfo.visibility = View.GONE
        llChildInfo.visibility = View.GONE
        llEmergencyInfo.visibility = View.GONE
        llDocuments.visibility = View.GONE
        llReview.visibility = View.GONE

        // Show current step
        when (currentStep) {
            1 -> {
                llParentInfo.visibility = View.VISIBLE
                btnNext.text = "Next"
            }
            2 -> {
                llChildInfo.visibility = View.VISIBLE
                btnNext.text = "Next"
            }
            3 -> {
                llEmergencyInfo.visibility = View.VISIBLE
                btnNext.text = "Next"
            }
            4 -> {
                llDocuments.visibility = View.VISIBLE
                btnNext.text = "Next"
            }
            5 -> {
                llReview.visibility = View.VISIBLE
                btnNext.text = "Complete Registration"
                showReviewData()
            }
        }

        updateNextButton() // Update button state after changing step visibility
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            1 -> validateParentInfo()
            2 -> validateChildInfo()
            3 -> validateEmergencyInfo()
            4 -> validateDocuments() // Still a placeholder, always returns true
            5 -> true // Review step is always valid
            else -> false
        }
    }

    private fun validateParentInfo(): Boolean {
        val name = etParentName.text.toString().trim()
        val email = etParentEmail.text.toString().trim()
        val phone = etParentPhone.text.toString().trim()
        val password = etRegisterPassword.text.toString().trim() // New: password

        when {
            name.isEmpty() -> {
                etParentName.error = "Name is required"
                return false
            }
            email.isEmpty() -> {
                etParentEmail.error = "Email is required"
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                etParentEmail.error = "Invalid email format"
                return false
            }
            phone.isEmpty() -> {
                etParentPhone.error = "Phone number is required"
                return false
            }
            password.isEmpty() || password.length < 6 -> { // New: password validation
                etRegisterPassword.error = "Password must be at least 6 characters"
                return false
            }
        }
        return true
    }

    private fun validateChildInfo(): Boolean {
        val name = etChildName.text.toString().trim()
        val age = etChildAge.text.toString().trim()

        when {
            name.isEmpty() -> {
                etChildName.error = "Child's name is required"
                return false
            }
            age.isEmpty() -> {
                etChildAge.error = "Child's age is required"
                return false
            }
        }
        return true
    }

    private fun validateEmergencyInfo(): Boolean {
        val emergencyContact = etEmergencyContact.text.toString().trim()

        if (emergencyContact.isEmpty()) {
            etEmergencyContact.error = "Emergency contact is required"
            return false
        }
        return true
    }

    private fun validateDocuments(): Boolean {
        // In a real app, you'd validate if documents were actually uploaded.
        // For this solution, we assume documents can be uploaded later or are optional for registration.
        return true
    }

    private fun updateNextButton() {
        btnNext.isEnabled = when (currentStep) {
            1 -> etParentName.text.isNotEmpty() && etParentEmail.text.isNotEmpty() &&
                    Patterns.EMAIL_ADDRESS.matcher(etParentEmail.text.toString().trim()).matches() &&
                    etParentPhone.text.isNotEmpty() && etRegisterPassword.text.length >= 6
            2 -> etChildName.text.isNotEmpty() && etChildAge.text.isNotEmpty()
            3 -> etEmergencyContact.text.isNotEmpty()
            4, 5 -> true // Documents and Review steps don't require specific field validation for 'Next'
            else -> false
        }
    }

    private fun showReviewData() {
        // Update review section with entered data
        findViewById<TextView>(R.id.tvReviewParentName)?.text = etParentName.text.toString()
        findViewById<TextView>(R.id.tvReviewParentEmail)?.text = etParentEmail.text.toString()
        findViewById<TextView>(R.id.tvReviewChildName)?.text = etChildName.text.toString()
        findViewById<TextView>(R.id.tvReviewChildAge)?.text = "${etChildAge.text} years old"
        // Also show phone, emergency contact, allergies if you add review TextViews for them
    }

    /**
     * Completes user registration by creating an account in Firebase Auth
     * and storing user/child data in Firestore.
     */
    private fun completeRegistration() {
        showLoading(true)

        val email = etParentEmail.text.toString().trim()
        val password = etRegisterPassword.text.toString().trim()
        val parentName = etParentName.text.toString().trim()
        val parentPhone = etParentPhone.text.toString().trim()
        val childName = etChildName.text.toString().trim()
        val childAge = etChildAge.text.toString().trim()
        val allergies = etAllergies.text.toString().trim()
        val emergencyContact = etEmergencyContact.text.toString().trim()


        // 1. Create user in Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { authTask ->
                if (authTask.isSuccessful) {
                    val firebaseUser = authTask.result?.user
                    firebaseUser?.let { user ->
                        val userId = user.uid

                        // 2. Create User object for Firestore
                        val newUser = User(
                            id = userId,
                            name = parentName,
                            email = email,
                            phone = parentPhone,
                            role = UserRole.PARENT.name, // Default role for registration
                            profileImageUrl = "", // Can be updated later
                            isActive = true,
                            createdAt = System.currentTimeMillis()
                        )

                        // 3. Create Child object for Firestore
                        val newChild = Child(
                            id = firestoreDb.collection("children")
                                .document().id, // Generate a new ID for the child
                            name = childName,
                            parentId = userId, // Link child to parent's Firebase UID
                            teacherId = "", // Assign teacher later
                            dateOfBirth = childAge, // Using age as DOB for simplicity, ideally a proper date.
                            allergies = allergies,
                            medicalNotes = "",
                            emergencyContact = emergencyContact,
                            profileImageUrl = "", // Can be updated later
                            isActive = true,
                            childId = TODO()
                        )

                        // Use a Batch Write for atomic operations: Save User and Child data
                        val batch = firestoreDb.batch()

                        // Add parent user data to 'users' collection
                        val userRef = firestoreDb.collection("users").document(userId)
                        batch.set(userRef, newUser)

                        // Add child data to 'children' collection
                        val childRef = firestoreDb.collection("children").document(newChild.id)
                        batch.set(childRef, newChild)

                        batch.commit()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                // Log in the new user in SharedPrefManager (optional, as Firebase Auth manages it)
                                // sharedPrefManager.saveUser(newUser) // Not needed if always fetching from Firestore
                                navigateToHome()
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                                // Optionally delete Firebase Auth user if Firestore save fails
                                user.delete()
                            }
                    }
                } else {
                    // Registration failed in Firebase Auth
                    showLoading(false)
                    val errorMessage = authTask.exception?.message ?: "Registration failed."
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showLoading(show: Boolean) {
        btnNext.isEnabled = !show
        btnNext.text = if (show) "Completing Registration..." else "Complete Registration"
    }

    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}