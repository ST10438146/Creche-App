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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

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

    // New: upcoming events list + listener
    private val upcomingEvents = mutableListOf<Event>()
    private lateinit var eventsAdapter: EventAdapter
    private var eventsListener: ListenerRegistration? = null

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

        setupRecentActivities() // now shows upcoming events
        setupQuickActionListeners()

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
                        tvQuickStats.text = "$childrenCount children present • $activitiesCount events scheduled"
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

    /**
     * Previously "Recent Activities" — now shows upcoming events / notices.
     * Shows the next 5 upcoming events with a realtime listener.
     */
    private fun setupRecentActivities() {
        rvRecentActivities.layoutManager = LinearLayoutManager(context)

        eventsAdapter = EventAdapter(upcomingEvents, { event ->
            // On click, show details
            showEventDetailsDialog(event)
        })
        rvRecentActivities.adapter = eventsAdapter

        // Query: events with date >= today (date stored as "yyyy-MM-dd"), ordered by date/time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Calendar.getInstance().time)

        // Listen for upcoming events (limit to next 5)
        eventsListener = firestoreDb.collection("events")
            .whereGreaterThanOrEqualTo("date", todayStr)
            .orderBy("date")
            .orderBy("time")
            .limit(5)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e(TAG, "Error loading upcoming events", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    upcomingEvents.clear()
                    for (doc in snapshots.documents) {
                        val event = doc.toObject(Event::class.java)
                        event?.let { upcomingEvents.add(it) }
                    }
                    eventsAdapter.notifyDataSetChanged()

                    // If no upcoming events show a subtle placeholder item (optional toast)
                    if (upcomingEvents.isEmpty()) {
                        // You might prefer to show an empty state view in the UI instead
                        Log.d(TAG, "No upcoming events found")
                    }
                }
            }
    }

    private fun showEventDetailsDialog(event: Event) {
        val message = StringBuilder()
            .append("Date: ").append(event.date).append("\n")
            .append("Time: ").append(event.time).append("\n")
            .append("Location: ").append(event.location.ifEmpty { "-" }).append("\n\n")
            .append(event.description)
            .toString()

        AlertDialog.Builder(requireContext())
            .setTitle(event.title.ifEmpty { "Event" })
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Open Events") { _, _ ->
                // Open full Events screen for more context
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
            .show()
    }

    override fun onDestroyView() {
        // remove listeners to avoid leaks and repeated updates
        userListener?.remove()
        userListener = null

        eventsListener?.remove()
        eventsListener = null

        super.onDestroyView()
    }
}
