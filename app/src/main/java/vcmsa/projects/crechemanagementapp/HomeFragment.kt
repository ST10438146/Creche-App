package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvQuickStats: TextView
    private lateinit var rvRecentActivities: RecyclerView // Will remain mostly for structure
    private lateinit var ivProfileImagePlaceholder: ImageView // Added for displaying profile image
    private lateinit var sharedPrefManager: SharedPrefManager

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()

        initViews(view)
        setupData()
    }

    private fun initViews(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvQuickStats = view.findViewById(R.id.tvQuickStats)
        rvRecentActivities = view.findViewById(R.id.rvRecentActivities)
        ivProfileImagePlaceholder = view.findViewById(R.id.ivProfileImage) // Assuming you'll add an ID to the ImageView in fragment_home.xml for profile pic

        sharedPrefManager = SharedPrefManager.getInstance(requireContext())
    }

    private fun setupData() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Fetch user data from Firestore
            firestoreDb.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(context, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)
                        user?.let {
                            tvWelcome.text = "Welcome back, ${it.name}!"

                            // Setup quick stats based on user role
                            when (it.role) {
                                UserRole.PARENT -> {
                                    // For parents, fetch child status from Firestore
                                    firestoreDb.collection("children")
                                        .whereEqualTo("parentId", it.id)
                                        .get()
                                        .addOnSuccessListener { childrenSnapshot ->
                                            if (!childrenSnapshot.isEmpty) {
                                                // Assuming one child per parent for simplicity in quick stats
                                                val childName = childrenSnapshot.documents[0].getString("name") ?: "Your child"
                                                tvQuickStats.text = "$childName is checked in • Next pickup: 5:00 PM" // Placeholder for attendance status
                                            } else {
                                                tvQuickStats.text = "No child registered yet."
                                            }
                                        }
                                        .addOnFailureListener { childError ->
                                            tvQuickStats.text = "Error loading child info."
                                            Toast.makeText(context, "Error loading child data: ${childError.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                UserRole.STAFF -> {
                                    // Fetch count of children and activities from Firestore
                                    firestoreDb.collection("children").get().addOnSuccessListener { children ->
                                        val childrenCount = children.size()
                                        firestoreDb.collection("events").get().addOnSuccessListener { events ->
                                            val activitiesCount = events.size() // Assuming events are activities
                                            tvQuickStats.text = "$childrenCount children present • $activitiesCount activities scheduled"
                                        }
                                    }
                                }
                                UserRole.ADMIN -> {
                                    // Fetch total children and staff members from Firestore
                                    firestoreDb.collection("children").get().addOnSuccessListener { children ->
                                        val childrenCount = children.size()
                                        firestoreDb.collection("users").whereEqualTo("role", UserRole.STAFF.name).get().addOnSuccessListener { staff ->
                                            val staffCount = staff.size()
                                            tvQuickStats.text = "$childrenCount total children • $staffCount staff members • R12,500 monthly revenue (Mock)"
                                        }
                                    }
                                }
                            }
                            // Load profile image (if any) - not fully implemented in this beginner solution
                            // if (it.profileImageUrl.isNotEmpty()) {
                            //     Glide.with(this).load(it.profileImageUrl).into(ivProfileImagePlaceholder)
                            // }
                        }
                    } else {
                        // User document not found, log out or show error
                        Toast.makeText(context, "User data missing from Firestore.", Toast.LENGTH_SHORT).show()
                        firebaseAuth.signOut()
                        startActivity(Intent(activity, LoginActivity::class.java))
                        activity?.finish()
                    }
                }
        } else {
            // No current user, redirect to login
            startActivity(Intent(activity, LoginActivity::class.java))
            activity?.finish()
        }

        setupRecentActivities() // Placeholder for recent activities
    }

    private fun setupRecentActivities() {
        rvRecentActivities.layoutManager = LinearLayoutManager(context)
        // In a real app, you'd load actual data here from Firestore
        // For now, we'll leave the RecyclerView empty or with mock data
    }
}