package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.Intent
import android.util.Log


class HomeActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sharedPrefManager = SharedPrefManager.getInstance(this)

        initViews()
        setupBottomNavigation()
        checkUserRoleAndRedirect()

        // Show default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }
    private fun checkUserRoleAndRedirect() {
        AuthHelper.hasRole(UserRole.ADMIN) { isAdmin ->
            Log.d("RoleDebug", "Is Admin: $isAdmin")
            if (isAdmin) {
                // Redirect to AdminHomeActivity
                val intent = Intent(this, AdminHomeActivity::class.java)
                startActivity(intent)
                finish() // Prevents returning to this activity
                return@hasRole
            }
            else {
                // ... check for other roles
                AuthHelper.hasRole(UserRole.STAFF) { isStaff ->
                    if (isStaff) {
                        // Redirect to StaffHomeActivity
                        val intent = Intent(this, StaffHomeActivity::class.java)
                        startActivity(intent)
                        finish()
                        return@hasRole
                    }

                    // If not Admin or Staff, they must be a Parent
                    AuthHelper.hasRole(UserRole.PARENT) { isParent ->
                        if (isParent) {
                            // Redirect to ParentHomeActivity
                            val intent = Intent(this, ParentHomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }

        }
    }
    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_attendance -> {
                    loadFragment(AttendanceFragment())
                    true
                }
                R.id.nav_events -> {
                    loadFragment(EventsFragment())
                    true
                }
                R.id.nav_payments -> {
                    loadFragment(PaymentsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
             .commit()
    }
}