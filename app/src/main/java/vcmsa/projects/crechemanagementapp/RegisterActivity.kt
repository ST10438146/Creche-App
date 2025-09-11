package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RegisterActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Shared Prefs
    private lateinit var sharedPrefManager: SharedPrefManager

    // Role selection - use your project's UserRole enum
    private var selectedUserRole: UserRole = UserRole.PARENT

    // Steps
    private var currentStep = 1
    private var totalSteps = 5 // default to parent flow

    // Views
    private lateinit var roleSelectorContainer: LinearLayout
    private lateinit var adminCard: LinearLayout
    private lateinit var staffCard: LinearLayout
    private lateinit var parentCard: LinearLayout
    private lateinit var tvStepProgress: TextView

    // Parent fields
    private lateinit var llParentInfo: LinearLayout
    private lateinit var etParentName: EditText
    private lateinit var etParentEmail: EditText
    private lateinit var etParentPhone: EditText
    private lateinit var etParentPassword: EditText

    // Child fields
    private lateinit var llChildInfo: LinearLayout
    private lateinit var etChildName: EditText
    private lateinit var etChildAge: EditText

    // Emergency
    private lateinit var llEmergencyInfo: LinearLayout
    private lateinit var etEmergencyContact: EditText
    private lateinit var etEmergencyPhone: EditText

    // Documents (placeholder)
    private lateinit var llDocuments: LinearLayout
    private lateinit var tvDocsPlaceholder: TextView

    // Staff fields
    private lateinit var llStaffInfo: LinearLayout
    private lateinit var etStaffName: EditText
    private lateinit var etStaffEmail: EditText
    private lateinit var etStaffPhone: EditText
    private lateinit var etStaffPosition: EditText
    private lateinit var etStaffId: EditText
    private lateinit var etStaffPassword: EditText

    // Admin fields
    private lateinit var llAdminInfo: LinearLayout
    private lateinit var etAdminName: EditText
    private lateinit var etAdminEmail: EditText
    private lateinit var etAdminPhone: EditText
    private lateinit var etAdminCode: EditText
    private lateinit var etAdminPassword: EditText

    // Review
    private lateinit var llReview: LinearLayout
    private lateinit var tvReviewData: TextView

    // Buttons
    private lateinit var btnNext: Button
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // SharedPrefManager
        sharedPrefManager = SharedPrefManager.getInstance(this)

        bindViews()
        readIntentRole()
        setupRoleSelection()
        setupButtons()
        attachTextWatchers()
        showCurrentStep()
    }

    private fun bindViews() {
        roleSelectorContainer = findViewById(R.id.roleSelectorContainer)
        adminCard = findViewById(R.id.adminCard)
        staffCard = findViewById(R.id.staffCard)
        parentCard = findViewById(R.id.parentCard)
        tvStepProgress = findViewById(R.id.tvStepProgress)

        // Parent
        llParentInfo = findViewById(R.id.llParentInfo)
        etParentName = findViewById(R.id.etParentName)
        etParentEmail = findViewById(R.id.etParentEmail)
        etParentPhone = findViewById(R.id.etParentPhone)
        etParentPassword = findViewById(R.id.etParentPassword)

        // Child
        llChildInfo = findViewById(R.id.llChildInfo)
        etChildName = findViewById(R.id.etChildName)
        etChildAge = findViewById(R.id.etChildAge)

        // Emergency
        llEmergencyInfo = findViewById(R.id.llEmergencyInfo)
        etEmergencyContact = findViewById(R.id.etEmergencyContact)
        etEmergencyPhone = findViewById(R.id.etEmergencyPhone)

        // Documents
        llDocuments = findViewById(R.id.llDocuments)
        tvDocsPlaceholder = findViewById(R.id.tvDocsPlaceholder)

        // Staff
        llStaffInfo = findViewById(R.id.llStaffInfo)
        etStaffName = findViewById(R.id.etStaffName)
        etStaffEmail = findViewById(R.id.etStaffEmail)
        etStaffPhone = findViewById(R.id.etStaffPhone)
        etStaffPosition = findViewById(R.id.etStaffPosition)
        etStaffId = findViewById(R.id.etStaffId)
        etStaffPassword = findViewById(R.id.etStaffPassword)

        // Admin
        llAdminInfo = findViewById(R.id.llAdminInfo)
        etAdminName = findViewById(R.id.etAdminName)
        etAdminEmail = findViewById(R.id.etAdminEmail)
        etAdminPhone = findViewById(R.id.etAdminPhone)
        etAdminCode = findViewById(R.id.etAdminCode)
        etAdminPassword = findViewById(R.id.etAdminPassword)

        // Review
        llReview = findViewById(R.id.llReview)
        tvReviewData = findViewById(R.id.tvReviewData)

        // Buttons
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun readIntentRole() {
        intent?.getStringExtra("USER_ROLE")?.let { roleName ->
            selectedUserRole = try {
                UserRole.valueOf(roleName)
            } catch (e: Exception) {
                UserRole.PARENT
            }
            // If role passed in, hide role selector and preselect flow
            roleSelectorContainer.visibility = View.GONE
            // set steps based on role
            totalSteps = when (selectedUserRole) {
                UserRole.PARENT -> 5
                UserRole.STAFF, UserRole.ADMIN -> 2
            }
        }
    }

    private fun setupRoleSelection() {
        // If no role came from Login, allow user to choose
        if (intent?.getStringExtra("USER_ROLE") == null) {
            roleSelectorContainer.visibility = View.VISIBLE
        } else {
            roleSelectorContainer.visibility = View.GONE
        }

        adminCard.setOnClickListener { selectRole(UserRole.ADMIN) }
        staffCard.setOnClickListener { selectRole(UserRole.STAFF) }
        parentCard.setOnClickListener { selectRole(UserRole.PARENT) }

        // apply initial selection UI
        selectRole(selectedUserRole)
    }

    private fun selectRole(role: UserRole) {
        selectedUserRole = role

        // visual cues: simple background color toggle
        adminCard.setBackgroundColor(if (role == UserRole.ADMIN) Color.LTGRAY else Color.WHITE)
        staffCard.setBackgroundColor(if (role == UserRole.STAFF) Color.LTGRAY else Color.WHITE)
        parentCard.setBackgroundColor(if (role == UserRole.PARENT) Color.LTGRAY else Color.WHITE)

        // show/hide form blocks and set steps
        when (role) {
            UserRole.PARENT -> {
                llParentInfo.visibility = View.VISIBLE
                llStaffInfo.visibility = View.GONE
                llAdminInfo.visibility = View.GONE
                totalSteps = 5
                currentStep = 1
            }
            UserRole.STAFF -> {
                llParentInfo.visibility = View.GONE
                llStaffInfo.visibility = View.VISIBLE
                llAdminInfo.visibility = View.GONE
                totalSteps = 2
                currentStep = 1
            }
            UserRole.ADMIN -> {
                llParentInfo.visibility = View.GONE
                llStaffInfo.visibility = View.GONE
                llAdminInfo.visibility = View.VISIBLE
                totalSteps = 2
                currentStep = 1
            }
        }
        showCurrentStep()
        updateNextButtonState()
    }

    private fun setupButtons() {
        btnNext.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                showCurrentStep()
            } else {
                // final step — complete registration
                completeRegistration()
            }
        }
        btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                showCurrentStep()
            } else {
                // if at beginning, just finish
                finish()
            }
        }
    }

    private fun showCurrentStep() {
        tvStepProgress.text = "Step $currentStep of $totalSteps"

        // hide everything then show what's needed
        llParentInfo.visibility = View.GONE
        llChildInfo.visibility = View.GONE
        llEmergencyInfo.visibility = View.GONE
        llDocuments.visibility = View.GONE
        llStaffInfo.visibility = View.GONE
        llAdminInfo.visibility = View.GONE
        llReview.visibility = View.GONE

        if (selectedUserRole == UserRole.PARENT) {
            when (currentStep) {
                1 -> llParentInfo.visibility = View.VISIBLE
                2 -> llChildInfo.visibility = View.VISIBLE
                3 -> llEmergencyInfo.visibility = View.VISIBLE
                4 -> llDocuments.visibility = View.VISIBLE
                5 -> {
                    llReview.visibility = View.VISIBLE
                    showReviewData()
                }
            }
        } else {
            // STAFF or ADMIN flows are short (info -> review)
            when (currentStep) {
                1 -> {
                    if (selectedUserRole == UserRole.STAFF) llStaffInfo.visibility = View.VISIBLE
                    else llAdminInfo.visibility = View.VISIBLE
                }
                2 -> {
                    llReview.visibility = View.VISIBLE
                    showReviewData()
                }
            }
        }
        updateNextButtonState()
    }

    private fun attachTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // every keystroke re-evaluates the button state
                updateNextButtonState()
                // refresh review text when on the review step
                if (currentStep == totalSteps) {
                    showReviewData()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        // Parent
        etParentName.addTextChangedListener(watcher)
        etParentEmail.addTextChangedListener(watcher)
        etParentPhone.addTextChangedListener(watcher)
        etParentPassword.addTextChangedListener(watcher)
        etChildName.addTextChangedListener(watcher)
        etChildAge.addTextChangedListener(watcher)
        etEmergencyContact.addTextChangedListener(watcher)
        etEmergencyPhone.addTextChangedListener(watcher)

        // Staff
        etStaffName.addTextChangedListener(watcher)
        etStaffEmail.addTextChangedListener(watcher)
        etStaffPhone.addTextChangedListener(watcher)
        etStaffPosition.addTextChangedListener(watcher)
        etStaffId.addTextChangedListener(watcher)
        etStaffPassword.addTextChangedListener(watcher)

        // Admin
        etAdminName.addTextChangedListener(watcher)
        etAdminEmail.addTextChangedListener(watcher)
        etAdminPhone.addTextChangedListener(watcher)
        etAdminCode.addTextChangedListener(watcher)
        etAdminPassword.addTextChangedListener(watcher)

        // run initial check
        updateNextButtonState()
    }

    private fun updateNextButtonState() {
        btnNext.isEnabled = when (selectedUserRole) {
            UserRole.PARENT -> when (currentStep) {
                1 -> isParentStep1Valid()
                2 -> isChildStepValid()
                3 -> isEmergencyStepValid()
                4 -> true
                5 -> true
                else -> false
            }
            UserRole.STAFF -> when (currentStep) {
                1 -> isStaffStepValid()
                2 -> true
                else -> false
            }
            UserRole.ADMIN -> when (currentStep) {
                1 -> isAdminStepValid()
                2 -> true
                else -> false
            }
        }
        btnNext.text = if (currentStep < totalSteps) "Next" else "Complete Registration"
        btnBack.text = if (currentStep > 1) "Back" else "Cancel"
    }

    // Validation helpers
    private fun isParentStep1Valid(): Boolean {
        val name = etParentName.text.toString().trim()
        val email = etParentEmail.text.toString().trim()
        val phone = etParentPhone.text.toString().trim()
        val pw = etParentPassword.text.toString()
        return name.isNotEmpty() && email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                phone.isNotEmpty() && pw.length >= 6
    }

    private fun isChildStepValid(): Boolean {
        val cname = etChildName.text.toString().trim()
        val cage = etChildAge.text.toString().trim()
        return cname.isNotEmpty() && cage.isNotEmpty()
    }

    private fun isEmergencyStepValid(): Boolean {
        val contact = etEmergencyContact.text.toString().trim()
        val phone = etEmergencyPhone.text.toString().trim()
        return contact.isNotEmpty() && phone.isNotEmpty()
    }

    private fun isStaffStepValid(): Boolean {
        val name = etStaffName.text.toString().trim()
        val email = etStaffEmail.text.toString().trim()
        val phone = etStaffPhone.text.toString().trim()
        val pw = etStaffPassword.text.toString()
        return name.isNotEmpty() && email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                phone.isNotEmpty() && pw.length >= 6
    }

    private fun isAdminStepValid(): Boolean {
        val name = etAdminName.text.toString().trim()
        val email = etAdminEmail.text.toString().trim()
        val phone = etAdminPhone.text.toString().trim()
        val pw = etAdminPassword.text.toString()
        return name.isNotEmpty() && email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                phone.isNotEmpty() && pw.length >= 6
    }

    private fun showReviewData() {
        val sb = StringBuilder()
        sb.append("Role: ${selectedUserRole.name}\n\n")
        when (selectedUserRole) {
            UserRole.PARENT -> {
                sb.append("Parent name: ${etParentName.text}\n")
                sb.append("Email: ${etParentEmail.text}\n")
                sb.append("Phone: ${etParentPhone.text}\n\n")
                sb.append("Child name: ${etChildName.text}\n")
                sb.append("Child age: ${etChildAge.text}\n\n")
                sb.append("Emergency contact: ${etEmergencyContact.text} (${etEmergencyPhone.text})\n\n")
                sb.append("Documents: ${tvDocsPlaceholder.text}\n")
            }
            UserRole.STAFF -> {
                sb.append("Name: ${etStaffName.text}\n")
                sb.append("Email: ${etStaffEmail.text}\n")
                sb.append("Phone: ${etStaffPhone.text}\n")
                sb.append("Position: ${etStaffPosition.text}\n")
                sb.append("Staff ID: ${etStaffId.text}\n")
            }
            UserRole.ADMIN -> {
                sb.append("Name: ${etAdminName.text}\n")
                sb.append("Email: ${etAdminEmail.text}\n")
                sb.append("Phone: ${etAdminPhone.text}\n")
                sb.append("Admin Code: ${etAdminCode.text}\n")
            }
        }
        tvReviewData.text = sb.toString()
        updateNextButtonState()
    }

    private fun completeRegistration() {
        showLoading(true)

        when (selectedUserRole) {
            UserRole.PARENT -> handleParentRegistration()
            UserRole.STAFF, UserRole.ADMIN -> handleStaffOrAdminRegistration()
        }
    }

    private fun handleParentRegistration() {
        val email = etParentEmail.text.toString().trim()
        val password = etParentPassword.text.toString()
        val name = etParentName.text.toString().trim()
        val phone = etParentPhone.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: UUID.randomUUID().toString()

                    val user = User(
                        uid = uid,
                        id = uid,
                        name = name,
                        email = email,
                        phone = phone,
                        role = UserRole.PARENT.name,
                        profileImageUrl = "",
                        isEnabled = true,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )

                    // Save user and child in a batch (simple example)
                    val userRef = db.collection("users").document(uid)
                    val childRef = db.collection("children").document() // new doc
                    val childPayload = mapOf(
                        "id" to childRef.id,
                        "parentId" to uid,
                        "name" to etChildName.text.toString().trim(),
                        "age" to etChildAge.text.toString().trim(),
                        "createdAt" to System.currentTimeMillis()
                    )

                    val batch = db.batch()
                    batch.set(userRef, user)
                    batch.set(childRef, childPayload)

                    batch.commit().addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(this, "Parent registration successful", Toast.LENGTH_LONG).show()

                        // Save just-created user to SharedPref for biometric caching
                        try {
                            sharedPrefManager.saveUser(user)
                        } catch (e: Exception) {
                            Log.w("RegisterActivity", "Failed to save user to SharedPref: ${e.message}")
                        }

                        navigateToHomeAfterRegistration()
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Error saving data: ${e.message}", Toast.LENGTH_LONG).show()
                        // Rollback auth user if you want
                        firebaseUser?.delete()
                    }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleStaffOrAdminRegistration() {
        val isStaff = selectedUserRole == UserRole.STAFF
        val email = if (isStaff) etStaffEmail.text.toString().trim() else etAdminEmail.text.toString().trim()
        val password = if (isStaff) etStaffPassword.text.toString() else etAdminPassword.text.toString()
        val name = if (isStaff) etStaffName.text.toString().trim() else etAdminName.text.toString().trim()
        val phone = if (isStaff) etStaffPhone.text.toString().trim() else etAdminPhone.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    val uid = firebaseUser?.uid ?: UUID.randomUUID().toString()

                    val user = User(
                        uid = uid,
                        id = uid,
                        name = name,
                        email = email,
                        phone = phone,
                        role = selectedUserRole.name,
                        profileImageUrl = "",
                        isEnabled = true,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )

                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener {
                            showLoading(false)
                            Toast.makeText(this, "${selectedUserRole.name} registration successful", Toast.LENGTH_LONG).show()

                            // Save the newly created user to SharedPrefManager so biometric login works immediately
                            try {
                                sharedPrefManager.saveUser(user)
                            } catch (e: Exception) {
                                Log.w("RegisterActivity", "Failed to save user to SharedPref: ${e.message}")
                            }

                            navigateToHomeAfterRegistration()
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            Toast.makeText(this, "Error saving user: ${e.message}", Toast.LENGTH_LONG).show()
                            // cleanup auth user
                            firebaseUser?.delete()
                        }
                } else {
                    showLoading(false)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun navigateToHomeAfterRegistration() {
        // If the user is auto-signed-in after registering, redirect based on role stored in Firestore.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // No signed-in user — go back to login
            Toast.makeText(this, "Registration complete. Please log in.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Read the user's role from Firestore (document should already exist because we just wrote it)
        val uid = currentUser.uid
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                showLoading(false)
                val roleString = doc?.getString("role") ?: UserRole.PARENT.name
                val role = try {
                    UserRole.valueOf(roleString)
                } catch (e: Exception) {
                    UserRole.PARENT
                }

                when (role) {
                    UserRole.ADMIN -> startActivity(Intent(this, AdminHomeActivity::class.java))
                    UserRole.STAFF, UserRole.PARENT -> startActivity(Intent(this, HomeActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener { e ->
                // If we can't read the role for any reason, fall back to the shared HomeActivity
                showLoading(false)
                Toast.makeText(this, "Registered but failed to read role; continuing to Home.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnNext.isEnabled = !show
        btnBack.isEnabled = !show
    }
}
