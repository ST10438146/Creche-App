package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TakeAttendanceAdapter(
    private val attendanceList: List<User>,
    private val users: List<User>,
    private val attendanceRecords: MutableMap<String, Boolean>
) : RecyclerView.Adapter<TakeAttendanceAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val email: TextView = v.findViewById(R.id.tvEmail)
        val checkBox: CheckBox = v.findViewById(R.id.cbPresent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_user, parent, false) // make a simple row layout
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = users[pos]
        h.name.text = u.name
        h.email.text = u.email
        h.checkBox.setOnCheckedChangeListener(null)
        h.checkBox.isChecked = attendanceRecords[u.id] == true
        h.checkBox.setOnCheckedChangeListener { _, checked ->
            attendanceRecords[u.id] = checked
        }
    }
    override fun getItemCount() = users.size
}