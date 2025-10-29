package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

class UserEventsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: SimpleListAdapter
    private var userId: String? = null

    companion object {
        fun newInstance(userId: String) = UserEventsFragment().apply {
            arguments = Bundle().apply { putString("userId", userId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val progress = view.findViewById<View>(R.id.progressBar)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SimpleListAdapter(emptyList(), binder = { map, holder ->
            holder.titleTv.text = map["title"]?.toString() ?: map["description"]?.toString() ?: "Event"
            holder.subtitleTv.text = "Date: ${map["date"] ?: "N/A"}"
        })
        recycler.adapter = adapter

        if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing userId", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE
        // If your events have a userId field or participant array; adapt accordingly
        firestore.collection("events")
            .whereEqualTo("userId", userId) // if you instead store participants: use .whereArrayContains("participants", userId)
            .orderBy("date")
            .get()
            .addOnSuccessListener { res ->
                progress.visibility = View.GONE
                adapter.update(res.map { it.data.toMap() })
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
