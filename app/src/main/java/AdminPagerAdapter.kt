package vcmsa.projects.crechemanagementapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class AdminPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    // 0: Users, 1: Events, 2: Attendance, 3: Payments
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AdminUsersFragment()
            1 -> EventsFragment() // reuse existing EventsFragment
            2 -> AttendanceFragment() // if present; replace with your fragment class
            3 -> PaymentsFragment()   // if present; replace with your fragment class
            else -> Fragment()
        }
    }
}
