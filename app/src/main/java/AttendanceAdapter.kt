package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(
    private val attendanceList: List<Attendance>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
        val tvCheckInTime: TextView = itemView.findViewById(R.id.tvHistoryCheckInTime)
        val tvCheckOutTime: TextView = itemView.findViewById(R.id.tvHistoryCheckOutTime)
        val tvStatus: TextView = itemView.findViewById(R.id.tvHistoryStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_history, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val a = attendanceList[position]
        holder.tvDate.text = a.date
        holder.tvCheckInTime.text = a.checkInTime?.let { formatTime(it) } ?: "—"
        holder.tvCheckOutTime.text = a.checkOutTime?.let { formatTime(it) } ?: "—"
        holder.tvStatus.text = a.status
        // keep your status color logic
    }

    override fun getItemCount(): Int = attendanceList.size

    private fun formatTime(ts: Long) =
        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(ts))
}