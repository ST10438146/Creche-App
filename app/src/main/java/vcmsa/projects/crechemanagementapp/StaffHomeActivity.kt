package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.crechemanagementapp.databinding.ActivityStaffHomeBinding

class StaffHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffHomeBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.manageAttendanceButton.setOnClickListener {
            // TODO: Implement the logic to navigate to the attendance management screen.
            // For example:
            // val intent = Intent(this, AttendanceActivity::class.java)
            // startActivity(intent)
        }
    }
}