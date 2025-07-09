package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(private val attendanceList: List<Attendance>) :
    RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

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
        val attendance = attendanceList[position]
        holder.tvDate.text = attendance.date
        holder.tvCheckInTime.text = attendance.checkInTime?.let { formatTime(it) } ?: "N/A"
        holder.tvCheckOutTime.text = attendance.checkOutTime?.let { formatTime(it) } ?: "N/A"
        holder.tvStatus.text = attendance.status
        // Optionally, color the status text
        val context = holder.itemView.context
        when (attendance.status) {
            "Checked In" -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            "Checked Out" -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            else -> holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        }
    }

    override fun getItemCount(): Int = attendanceList.size

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}