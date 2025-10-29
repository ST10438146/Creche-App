package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.crechemanagementapp.databinding.ActivityAdminDashboardBinding

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val auth = FirebaseAuth.getInstance()
    private val tabs = listOf("Users", "Attendance", "Events", "Payments")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = tabs.size
            override fun createFragment(position: Int) = when (position) {
                0 -> AdminUsersFragment()
                1 -> AdminAttendanceFragment()
                2 -> AdminEventsFragment()
                3 -> AdminPaymentsFragment()
                else -> AdminUsersFragment()
            }
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = tabs[pos]
        }.attach()

        binding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_logout -> {
                    auth.currentUser?.uid?.let { ActivityLogger.log(it, "ADMIN_LOGOUT", "Admin signed out") }
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
