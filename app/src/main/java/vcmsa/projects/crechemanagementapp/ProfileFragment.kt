package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnSettings: Button
    private lateinit var btnLogout: Button
    private lateinit var sharedPrefManager: SharedPrefManager

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private val TAG = "ProfileFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        initViews(view)
        setupClickListeners()
        loadUserData() // This will now load from Firestore in real-time
    }

    private fun initViews(view: View) {
        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        tvUserRole = view.findViewById(R.id.tvUserRole)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnLogout = view.findViewById(R.id.btnLogout)

        sharedPrefManager = SharedPrefManager.getInstance(requireContext())
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
    }
    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    /**
     * Loads user data from Firestore and updates the UI.
     * Uses a real-time listener to keep the profile updated.
     */
    private fun loadUserData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestoreDb.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Profile listener error: ${e.message}")
                        Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)
                        user?.let {
                            tvUserName.text = it.name.ifEmpty { "No name" }
                            tvUserEmail.text = it.email.ifEmpty { "No email" }

                            // Format role: e.g., "PARENT" -> "Parent"
                            val rawRole = it.role ?: ""
                            if (rawRole.isNotBlank()) {
                                tvUserRole.text = rawRole.lowercase().replaceFirstChar { ch -> ch.titlecase() }
                            } else {
                                tvUserRole.text = ""
                            }

                            // Load profile image if URL exists (uncomment if using Glide)
                            if (it.profileImageUrl.isNotEmpty()) {
                                // Glide.with(this).load(it.profileImageUrl).into(ivProfileImage)
                            } else {
                                ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
                            }
                        } ?: run {
                            Log.w(TAG, "User object was null after snapshot toObject()")
                            Toast.makeText(context, "Profile data missing.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Profile snapshot missing or does not exist")
                        Toast.makeText(context, "Profile data missing.", Toast.LENGTH_SHORT).show()
                        // Optionally force re-login if crucial data is missing
                        // performLogout()
                    }
                }
        } else {
            // No user logged in, redirect to login
            performLogout()
        }
    }

    /**
     * Logs out the user from Firebase and navigates to the Login screen.
     */
    private fun performLogout() {
        // Clear saved user from SharedPrefManager
        try {
            sharedPrefManager.clearUser()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear SharedPref user: ${e.message}")
        }

        // Sign out Firebase auth
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to sign out FirebaseAuth: ${e.message}")
        }

        // Navigate to login and clear back stack
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
}
