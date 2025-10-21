package vcmsa.projects.crechemanagementapp

import android.app.Activity
import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()
        sharedPrefManager = SharedPrefManager.getInstance(this)

        initViews()
        loadCurrentUserData()
        setupClickListeners()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            saveProfileUpdates()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentUserData() {
        val user = sharedPrefManager.getUser()
        user?.let {
            etName.setText(it.name)
            etPhone.setText(it.phone)
        }
    }

    private fun saveProfileUpdates() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val userId = firebaseAuth.currentUser?.uid ?: return

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val progress = ProgressDialog(this)
        progress.setMessage("Updating profile...")
        progress.setCancelable(false)
        progress.show()

        val userRef = firestoreDb.collection("users").document(userId)
        val updates = mapOf(
            "name" to name,
            "phone" to phone
        )

        userRef.update(updates)
            .addOnSuccessListener {
                progress.dismiss()
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                // Update local cache
                val updatedUser = sharedPrefManager.getUser()?.copy(
                    name = name,
                    phone = phone
                )
                if (updatedUser != null) {
                    sharedPrefManager.saveUser(updatedUser)
                }

                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                progress.dismiss()
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
