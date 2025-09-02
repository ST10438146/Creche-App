package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
// import com.bumptech.glide.Glide // For loading images from URL, add dependency if you use this


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
            Toast.makeText(context, "Edit Profile (Not implemented yet)", Toast.LENGTH_SHORT).show()
            // Implement logic to edit user profile data in Firestore
        }

        btnSettings.setOnClickListener {
            Toast.makeText(context, "Settings (Not implemented yet)", Toast.LENGTH_SHORT).show()
            // Implement logic for app settings
        }

        btnLogout.setOnClickListener {
            performLogout()
        }
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
                        Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)
                        user?.let {
                            tvUserName.text = it.name
                            tvUserEmail.text = it.email
                            tvUserRole.text = it.role
                                .lowercase()
                                .replaceFirstChar { ch -> ch.titlecase() }


                            // Load profile image if URL exists
                            // if (it.profileImageUrl.isNotEmpty()) {
                            //     Glide.with(this).load(it.profileImageUrl).into(ivProfileImage)
                            // } else {
                            //     ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
                            // }
                        }
                    } else {
                        Toast.makeText(context, "Profile data missing.", Toast.LENGTH_SHORT).show()
                        // Optionally force re-login if crucial data is missing
                        performLogout()
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
        sharedPrefManager.logout() // This now calls firebaseAuth.signOut() internally
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Clear back stack
        startActivity(intent)
        requireActivity().finish() // Finish current activity
    }
}