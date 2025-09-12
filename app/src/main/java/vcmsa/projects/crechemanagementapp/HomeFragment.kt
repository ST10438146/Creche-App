package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvQuickStats: TextView
    private lateinit var rvRecentActivities: RecyclerView
    private lateinit var ivProfileImagePlaceholder: ImageView

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var sharedPrefManager: SharedPrefManager

    private var userListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()
        sharedPrefManager = SharedPrefManager.getInstance(requireContext())

        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvQuickStats = view.findViewById(R.id.tvQuickStats)
        rvRecentActivities = view.findViewById(R.id.rvRecentActivities)
        ivProfileImagePlaceholder = view.findViewById(R.id.ivProfileImage)

        setupRecentActivities()

        val uid = firebaseAuth.currentUser?.uid
        if (uid == null) {
            // no user -> force login
            startActivity(Intent(requireActivity(), LoginActivity::class.java))
            requireActivity().finish()
            return
        }

        // Use cached user when available to avoid extra Firestore reads
        val cached = sharedPrefManager.getUser()
        if (cached != null) {
            tvWelcome.text = "Welcome back, ${cached.name}!"
            setupQuickStatsForRole(cached.role, cached.id)
        }

        // Listen for real-time user updates, but keep a single listener and remove it later
        userListener = firestoreDb.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    user?.let {
                        tvWelcome.text = "Welcome back, ${it.name}!"
                        setupQuickStatsForRole(it.role ?: UserRole.PARENT.name, it.id)
                        if (!it.profileImageUrl.isNullOrBlank()) {
                            // load image with Glide if available
                        } else {
                            ivProfileImagePlaceholder.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                        // update local cache so app starts faster next time
                        try { sharedPrefManager.saveUser(it) } catch (ex: Exception) { /* ignore */ }
                    }
                } else {
                    // Document missing -> log out to be safe
                    Toast.makeText(context, "User data missing.", Toast.LENGTH_SHORT).show()
                    firebaseAuth.signOut()
                    startActivity(Intent(activity, LoginActivity::class.java))
                    activity?.finish()
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
                    .addOnFailureListener {
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
                    }
                }
            }
            UserRole.ADMIN -> {
                firestoreDb.collection("children").get().addOnSuccessListener { children ->
                    val childrenCount = children.size()
                    firestoreDb.collection("users").whereEqualTo("role", UserRole.STAFF.name).get().addOnSuccessListener { staff ->
                        val staffCount = staff.size()
                        tvQuickStats.text = "$childrenCount total children • $staffCount staff members • R12,500 monthly revenue (Mock)"
                    }
                }
            }
            else -> { /* no-op */ }
        }
    }

    private fun setupRecentActivities() {
        rvRecentActivities.layoutManager = LinearLayoutManager(context)
        // adapter setup...
    }

    override fun onDestroyView() {
        // remove listener to avoid leaks and repeated updates
        userListener?.remove()
        userListener = null
        super.onDestroyView()
    }
}
