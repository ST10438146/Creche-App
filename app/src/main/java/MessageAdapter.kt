package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        val isSentByCurrentUser = msg.senderId == currentUserId

        // Format timestamp nicely
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = msg.timestamp?.let { timeFormat.format(it) } ?: ""

        // Reset visibility
        holder.sentLayout.visibility = View.GONE
        holder.receivedLayout.visibility = View.GONE

        if (isSentByCurrentUser) {
            // Sent message
            holder.sentLayout.visibility = View.VISIBLE
            holder.tvSentMessage.text = msg.content
            holder.tvSentReceiver.text = "To: ${msg.receiverName ?: "Parent"}"
            holder.tvSentTime.text = formattedTime
        } else {
            // Received message
            holder.receivedLayout.visibility = View.VISIBLE
            holder.tvReceivedMessage.text = msg.content
            holder.tvReceivedSender.text = msg.senderName ?: "Staff"
            holder.tvReceivedTime.text = formattedTime
        }
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sentLayout: LinearLayout = view.findViewById(R.id.sentLayout)
        val receivedLayout: LinearLayout = view.findViewById(R.id.receivedLayout)

        val tvSentReceiver: TextView = view.findViewById(R.id.tvSentReceiver)
        val tvSentMessage: TextView = view.findViewById(R.id.tvSentMessage)
        val tvSentTime: TextView = view.findViewById(R.id.tvSentTime)

        val tvReceivedSender: TextView = view.findViewById(R.id.tvReceivedSender)
        val tvReceivedMessage: TextView = view.findViewById(R.id.tvReceivedMessage)
        val tvReceivedTime: TextView = view.findViewById(R.id.tvReceivedTime)
    }
}
