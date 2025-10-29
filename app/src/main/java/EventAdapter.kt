package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private val eventList: MutableList<Event>,
    private val onItemClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        val tvEventTime: TextView = itemView.findViewById(R.id.tvEventTime)
        val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        val tvEventDescription: TextView = itemView.findViewById(R.id.tvEventDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        holder.tvEventTitle.text = event.title
        holder.tvEventDate.text = "Date: ${event.date}"
        holder.tvEventTime.text = "Time: ${event.time}"
        holder.tvEventLocation.text = event.location.ifEmpty { "Location: - " }
        holder.tvEventDescription.text = event.description

        holder.itemView.setOnClickListener {
            onItemClick(event)
        }
    }

    override fun getItemCount(): Int = eventList.size
}
