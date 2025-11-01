package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class MessagesFragment : Fragment() {

    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvMessages: RecyclerView
    private lateinit var rvSentMessages: RecyclerView
    private lateinit var messagesAdapter: MessageAdapter
    private lateinit var sentMessagesAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()
    private val sentMessagesList = mutableListOf<Message>()

    private lateinit var sendLayout: LinearLayout
    private lateinit var spnParents: Spinner
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var sentMessagesSection: LinearLayout

    private val parentMap = mutableMapOf<String, String>() // name → uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        firestoreDb = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvMessages = view.findViewById(R.id.rvMessages)
        rvMessages.layoutManager = LinearLayoutManager(requireContext())
        messagesAdapter = MessageAdapter(messagesList)
        rvMessages.adapter = messagesAdapter

        rvSentMessages = view.findViewById(R.id.rvSentMessages)
        rvSentMessages.layoutManager = LinearLayoutManager(requireContext())
        sentMessagesAdapter = MessageAdapter(sentMessagesList)
        rvSentMessages.adapter = sentMessagesAdapter

        sendLayout = view.findViewById(R.id.sendLayout)
        spnParents = view.findViewById(R.id.spnParents)
        etMessage = view.findViewById(R.id.etMessage)
        btnSend = view.findViewById(R.id.btnSend)
        sentMessagesSection = view.findViewById(R.id.sentMessagesSection)

        val currentUser = SharedPrefManager.getInstance(requireContext()).getUser()
        val isStaff = currentUser?.role.equals("STAFF", true) || currentUser?.role.equals("ADMIN", true)

        sendLayout.visibility = if (isStaff) View.VISIBLE else View.GONE
        sentMessagesSection.visibility = if (isStaff) View.VISIBLE else View.GONE

        if (isStaff) {
            loadParentsList()
            loadSentMessages()
        }

        loadReceivedMessages()

        btnSend.setOnClickListener {
            val selectedParentName = spnParents.selectedItem?.toString() ?: ""
            val receiverId = parentMap[selectedParentName] ?: ""
            val messageText = etMessage.text.toString().trim()

            if (receiverId.isEmpty() || messageText.isEmpty()) {
                Toast.makeText(context, "Select parent and type message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = hashMapOf(
                "senderId" to auth.currentUser?.uid,
                "senderName" to currentUser?.name,
                "receiverId" to receiverId,
                "receiverName" to selectedParentName,
                "content" to messageText,
                "timestamp" to Date()
            )

            firestoreDb.collection("messages")
                .add(message)
                .addOnSuccessListener {
                    Toast.makeText(context, "Message sent to $selectedParentName", Toast.LENGTH_SHORT).show()
                    etMessage.text.clear()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadParentsList() {
        firestoreDb.collection("users")
            .whereEqualTo("role", "PARENT")
            .get()
            .addOnSuccessListener { snapshot ->
                val parentNames = mutableListOf<String>()
                for (doc in snapshot.documents) {
                    val name = doc.getString("name") ?: "Unnamed"
                    val uid = doc.id
                    parentNames.add(name)
                    parentMap[name] = uid
                }

                if (parentNames.isEmpty()) parentNames.add("No parents found")

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, parentNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spnParents.adapter = adapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading parents: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadReceivedMessages() {
        val userId = auth.currentUser?.uid ?: return

        firestoreDb.collection("messages")
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    messagesList.clear()
                    for (doc in snapshots.documents) {
                        val msg = doc.toObject(Message::class.java)
                        msg?.let { messagesList.add(it) }
                    }
                    messagesAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun loadSentMessages() {
        val userId = auth.currentUser?.uid ?: return

        firestoreDb.collection("messages")
            .whereEqualTo("senderId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading sent messages: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    sentMessagesList.clear()
                    for (doc in snapshots.documents) {
                        val msg = doc.toObject(Message::class.java)
                        msg?.let { sentMessagesList.add(it) }
                    }
                    sentMessagesAdapter.notifyDataSetChanged()
                }
            }
    }
}
