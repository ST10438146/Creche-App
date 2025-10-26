package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private val TAG = "HomeFragment"

    private lateinit var tvWelcome: TextView
    private lateinit var tvQuickStats: TextView
    private lateinit var rvRecentActivities: RecyclerView
    private lateinit var ivProfileImagePlaceholder: ImageView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var sharedPrefManager: SharedPrefManager

    // Quick action containers
    private lateinit var llQuickAttendance: View
    private lateinit var llQuickEvents: View
    private lateinit var llQuickPayments: View
    private lateinit var llQuickMessages: View

    private var userListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init Firebase & local helpers
        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()
        sharedPrefManager = SharedPrefManager.getInstance(requireContext())

        // Bind views
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvQuickStats = view.findViewById(R.id.tvQuickStats)
        rvRecentActivities = view.findViewById(R.id.rvRecentActivities)
        ivProfileImagePlaceholder = view.findViewById(R.id.ivProfileImage)

        // Quick actions
        llQuickAttendance = view.findViewById(R.id.llQuickAttendance)
        llQuickEvents = view.findViewById(R.id.llQuickEvents)
        llQuickPayments = view.findViewById(R.id.llQuickPayments)
        llQuickMessages = view.findViewById(R.id.llQuickMessages)

        setupRecentActivities()

        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "No authenticated user found, redirecting to LoginActivity")
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        // Use cached user if available (fast startup)
        val cached = sharedPrefManager.getUser()
        if (cached != null) {
            tvWelcome.text = "Welcome back, ${cached.name}!"
            setupQuickStatsForRole(cached.role, cached.id)
        }

        // Setup quick-action click listeners (safe navigation)
        setupQuickActionListeners()

        // Listen to user document for live updates (single listener)
        userListener = firestoreDb.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error loading user data", e)
                    Toast.makeText(context, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    user?.let {
                        tvWelcome.text = "Welcome back, ${it.name}!"
                        setupQuickStatsForRole(it.role ?: UserRole.PARENT.name, it.id)
                        if (!it.profileImageUrl.isNullOrBlank()) {
                            // TODO: load image with Glide or other image loader
                        } else {
                            ivProfileImagePlaceholder.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                        // update local cache
                        try {
                            sharedPrefManager.saveUser(it)
                        } catch (ex: Exception) {
                            Log.w(TAG, "Failed to cache user locally", ex)
                        }
                    }
                } else {
                    // If the user document is missing, sign out for safety
                    Log.w(TAG, "User document missing for uid=$uid, signing out")
                    Toast.makeText(context, "User data missing.", Toast.LENGTH_SHORT).show()
                    firebaseAuth.signOut()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    activity?.finish()
                }
            }
    }

    private fun setupQuickActionListeners() {
        // Determine if the host activity has a fragment container we can replace
        val fragContainer = activity?.findViewById<View>(R.id.fragmentContainer)
        val hasFragContainer = fragContainer != null

        // Internal navigation helper: uses fragment container if available
        fun navigateToFragment(target: androidx.fragment.app.Fragment) {
            if (hasFragContainer) {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, target)
                    .addToBackStack(null)
                    .commit()
            } else {
                // No fragment container — fallback to Activity (adjust as needed)
                // For now show a short toast; replace with startActivity(...) if you have activities.
                Toast.makeText(requireContext(), "Opening screen...", Toast.LENGTH_SHORT).show()
            }
        }

        // Attendance quick action
        llQuickAttendance.setOnClickListener {
            try {
                val host = activity
                if (host is HomeActivity) {
                    host.openAttendance()
                } else {
                    // fallback (same as before)
                    parentFragmentManager.beginTransaction()
                        .setReorderingAllowed(true)
                        .replace(R.id.fragmentContainer, AttendanceFragment())
                        .addToBackStack(null)
                        .commit()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to navigate to Attendance", ex)
                Toast.makeText(requireContext(), "Unable to open Attendance", Toast.LENGTH_SHORT).show()
            }
        }

// Events quick action
        llQuickEvents.setOnClickListener {
            val host = activity
            if (host is HomeActivity) {
                host.openEvents()
            } else {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, EventsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

// Payments quick action
        llQuickPayments.setOnClickListener {
            val host = activity
            if (host is HomeActivity) {
                host.openPayments()
            } else {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentContainer, PaymentsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

// Messages quick action
        llQuickMessages.setOnClickListener {
            val host = activity
            if (host is HomeActivity) {
                host.openMessages()
            } else {
                Toast.makeText(requireContext(), "Messages not implemented yet.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun setupQuickStatsForRole(rawRole: String, userId: String) {
        when (runCatching { UserRole.valueOf(rawRole.uppercase()) }.getOrNull()) {
            UserRole.PARENT -> {
                // Fetch child (single quick get)
                firestoreDb.collection("children")
                    .whereEqualTo("parentId", userId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { childrenSnapshot ->
                        if (!childrenSnapshot.isEmpty) {
                            val childName = childrenSnapshot.documents[0].getString("name") ?: "Your child"
                            tvQuickStats.text = "$childName is checked in • Next pickup: 5:00 PM"
                        } else {
                            tvQuickStats.text = "No child registered yet."
                        }
                    }
                    .addOnFailureListener { ex ->
                        Log.w(TAG, "Error loading child info", ex)
                        tvQuickStats.text = "Error loading child info."
                    }
            }
            UserRole.STAFF -> {
                // lightweight counts (single-shot get)
                firestoreDb.collection("children").get().addOnSuccessListener { children ->
                    val childrenCount = children.size()
                    firestoreDb.collection("events").get().addOnSuccessListener { events ->
                        val activitiesCount = events.size()
                        tvQuickStats.text = "$childrenCount children present • $activitiesCount activities scheduled"
                    }.addOnFailureListener { ex ->
                        Log.w(TAG, "Error loading events count", ex)
                    }
                }.addOnFailureListener { ex ->
                    Log.w(TAG, "Error loading children count", ex)
                }
            }
            UserRole.ADMIN -> {
                firestoreDb.collection("children").get().addOnSuccessListener { children ->
                    val childrenCount = children.size()
                    firestoreDb.collection("users").whereEqualTo("role", UserRole.STAFF.name).get().addOnSuccessListener { staff ->
                        val staffCount = staff.size()
                        tvQuickStats.text = "$childrenCount total children • $staffCount staff members • R12,500 monthly revenue (Mock)"
                    }.addOnFailureListener { ex ->
                        Log.w(TAG, "Error loading staff count", ex)
                    }
                }.addOnFailureListener { ex ->
                    Log.w(TAG, "Error loading children count", ex)
                }
            }
            else -> { /* no-op */ }
        }
    }

    private fun setupRecentActivities() {
        rvRecentActivities.layoutManager = LinearLayoutManager(context)
        // TODO: set adapter and populate recent activity items
    }

    override fun onDestroyView() {
        // remove listener to avoid leaks and repeated updates
        userListener?.remove()
        userListener = null
        super.onDestroyView()
    }
}
