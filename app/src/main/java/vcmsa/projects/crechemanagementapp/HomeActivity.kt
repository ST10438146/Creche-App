package vcmsa.projects.crechemanagementapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private val TAG = "HomeActivity"
    private var bottomNavigation: BottomNavigationView? = null
    private lateinit var sharedPrefManager: SharedPrefManager
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // whether the layout contains a fragment container we can replace
    private var hasFragmentContainer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPrefManager = SharedPrefManager.getInstance(this)

        initViews()
        setupBottomNavigation()

        // Load user and start UI
        loadUserThenStart()
    }

    private fun initViews() {
        // Safe lookup: findViewById returns null if view not present in the layout
        bottomNavigation = findViewById(R.id.bottomNavigation)
        if (bottomNavigation == null) {
            Log.w(TAG, "bottomNavigation view not found in activity_home.xml. Please add a BottomNavigationView with id @id/bottomNavigation")
        }

        // Check for fragment container presence (FrameLayout or similar)
        val fragContainer = findViewById<View?>(R.id.fragmentContainer)
        hasFragmentContainer = fragContainer != null
        if (!hasFragmentContainer) {
            Log.i(TAG, "fragmentContainer id not found in layout — HomeActivity will not attempt fragment transactions.")
        }
    }

    private fun loadUserThenStart() {
        // Try cached user first
        val cached = sharedPrefManager.getUser()
        if (cached != null) {
            handleUserLoaded(cached)
            return
        }

        // If no cached user, fallback to Firebase current user
        val current = auth.currentUser
        if (current == null) {
            Log.w(TAG, "No signed-in Firebase user, redirecting to LoginActivity")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Fetch user doc once (non-realtime)
        firestore.collection("users").document(current.uid).get()
            .addOnSuccessListener { doc ->
                val user = doc?.toObject(User::class.java)?.copy(uid = current.uid, id = current.uid)
                if (user != null) {
                    // cache and proceed
                    try { sharedPrefManager.saveUser(user) } catch (e: Exception) { Log.w(TAG, "saveUser failed: ${e.message}") }
                    handleUserLoaded(user)
                } else {
                    Log.w(TAG, "User document not found or mapping returned null")
                    showHomeFragment(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch user doc: ${e.message}", e)
                showHomeFragment(null)
            }
    }

    private fun handleUserLoaded(user: User) {
        if (user.role.equals(UserRole.ADMIN.name, ignoreCase = true)) {
            // only Admins are redirected away from shared HomeActivity
            startActivity(Intent(this, AdminHomeActivity::class.java))
            finish()
            return
        }

        // show home fragment / UI for Staff and Parent
        showHomeFragment(user)
    }

    /**
     * Show the HomeFragment (or placeholder) and set bottom nav selected item.
     * Passes user info in fragment args if user != null.
     */
    private fun showHomeFragment(user: User?) {
        if (hasFragmentContainer) {
            val frag = HomeFragment()
            if (user != null) {
                val args = Bundle().apply {
                    putString("uid", user.uid)
                    putString("id", user.id)
                    putString("name", user.name)
                    putString("email", user.email)
                    putString("phone", user.phone)
                    putString("role", user.role)
                    putString("profileImageUrl", user.profileImageUrl)
                }
                frag.arguments = args
            }
            loadFragmentSafe(frag)
        } else {
            if (user != null) {
                Toast.makeText(this, "Welcome, ${user.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Welcome", Toast.LENGTH_SHORT).show()
            }
        }

        // set selected menu item if bottomNavigation exists and id present
        try {
            bottomNavigation?.selectedItemId = R.id.nav_home
        } catch (e: Exception) {
            // ignore if id not present.
        }
    }

    private fun setupBottomNavigation() {
        // If bottomNavigation view is missing, only log and return
        val nav = bottomNavigation
        if (nav == null) {
            Log.w(TAG, "BottomNavigationView is null — skipping setupBottomNavigation")
            return
        }

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                // Use these exact IDs in your menu XML or adjust them here to match your menu IDs.
                R.id.nav_home -> {
                    if (hasFragmentContainer) loadFragmentSafe(HomeFragment()) else showPlaceholder("Home")
                    true
                }
                R.id.nav_attendance -> {
                    if (hasFragmentContainer) openAttendance() else showPlaceholder("Attendance")
                    true
                }
                R.id.nav_events -> {
                    if (hasFragmentContainer) openEvents() else showPlaceholder("Events")
                    true
                }
                R.id.nav_profile -> {
                    if (hasFragmentContainer) loadFragmentSafe(ProfileFragment()) else showPlaceholder("Profile")
                    true
                }
                R.id.action_logout -> {
                    performLogout()
                    true
                }
                else -> {
                    Log.w(TAG, "Unhandled bottom nav id: ${item.itemId}")
                    false
                }
            }
        }
    }

    private fun showPlaceholder(name: String) {
        Toast.makeText(this, "$name tapped (no fragment container present).", Toast.LENGTH_SHORT).show()
    }

    /**
     * Perform fragment transaction only when fragmentContainer exists.
     * This avoids the crash you saw from calling replace(...) when container is missing.
     */
    private fun loadFragmentSafe(fragment: Fragment, addToBackStack: Boolean = false) {
        if (!hasFragmentContainer) {
            Log.w(TAG, "Attempted to load fragment but no fragmentContainer in layout")
            return
        }
        val tx = supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragmentContainer, fragment)
        if (addToBackStack) tx.addToBackStack(null)
        tx.commit()
    }

    private fun performLogout() {
        try { sharedPrefManager.clearUser() } catch (e: Exception) { Log.w(TAG, "clearUser failed: ${e.message}") }
        try { FirebaseAuth.getInstance().signOut() } catch (e: Exception) { Log.w(TAG, "Firebase signOut failed: ${e.message}") }
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // ---------------------------
    // Public navigation helpers
    // HomeFragment should call these instead of doing fragment transaction itself
    // ---------------------------

    /**
     * Open the Attendance screen.
     * Loads `AttendanceFragment` into the fragment container if present; otherwise starts `AttendanceManagementActivity`.
     * Also calls setResult(...) with an "opened" extra so callers can observe what was opened.
     */
    fun openAttendance() {
        try {
            if (hasFragmentContainer) {
                loadFragmentSafe(AttendanceFragment(), addToBackStack = true)
            } else {
                startActivity(Intent(this, AttendanceManagementActivity::class.java))
            }
            // inform caller (if this HomeActivity was started for result) what we opened
            setActivityOpenedResult("attendance")
        } catch (ex: Exception) {
            Log.e(TAG, "openAttendance failed", ex)
            Toast.makeText(this, "Unable to open Attendance", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open the Events screen.
     */
    fun openEvents() {
        try {
            if (hasFragmentContainer) {
                loadFragmentSafe(EventsFragment(), addToBackStack = true)
            } else {
                startActivity(Intent(this, EventsFragment::class.java)) // optional activity fallback
            }
            setActivityOpenedResult("events")
        } catch (ex: Exception) {
            Log.e(TAG, "openEvents failed", ex)
            Toast.makeText(this, "Unable to open Events", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open the Payments screen.
     */
    fun openPayments() {
        try {
            if (hasFragmentContainer) {
                loadFragmentSafe(PaymentsFragment(), addToBackStack = true)
            } else {
                startActivity(Intent(this, PaymentsFragment::class.java)) // optional fallback
            }
            setActivityOpenedResult("payments")
        } catch (ex: Exception) {
            Log.e(TAG, "openPayments failed", ex)
            Toast.makeText(this, "Unable to open Payments", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open Messages screen (if implemented) or show a friendly message.
     */
    fun openMessages() {
        try {
            if (hasFragmentContainer) {
                // If you add MessagesFragment in your project, replace the Toast with the fragment below:
                // loadFragmentSafe(MessagesFragment(), addToBackStack = true)
                // For now show a small placeholder behavior:
                Toast.makeText(this, "Messages not implemented yet.", Toast.LENGTH_SHORT).show()
            } else {
                // placeholder activity fallback
                Toast.makeText(this, "Messages not implemented yet.", Toast.LENGTH_SHORT).show()
            }
            setActivityOpenedResult("messages")
        } catch (ex: Exception) {
            Log.e(TAG, "openMessages failed", ex)
            Toast.makeText(this, "Unable to open Messages", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Send a small result Intent so a parent activity (if any) can detect which quick action was opened.
     * This mirrors "returning results" behavior — harmless if nobody is listening.
     */
    private fun setActivityOpenedResult(opened: String) {
        try {
            val resultIntent = Intent().apply {
                putExtra("opened", opened)
            }
            setResult(Activity.RESULT_OK, resultIntent)
        } catch (ex: Exception) {
            Log.w(TAG, "setActivityOpenedResult failed: ${ex.message}")
        }
    }
}
