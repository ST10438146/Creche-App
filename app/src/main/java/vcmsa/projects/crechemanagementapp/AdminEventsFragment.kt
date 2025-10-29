package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

class AdminEventsFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: SimpleListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val progress = view.findViewById<View>(R.id.progressBar)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SimpleListAdapter(emptyList(), binder = { map, holder ->
            val title = map["title"] ?: map["description"] ?: "Event"
            val date = map["date"] ?: "N/A"
            holder.titleTv.text = title.toString()
            holder.subtitleTv.text = "Date: $date"
        })
        recycler.adapter = adapter

        progress.visibility = View.VISIBLE
        firestore.collection("events").orderBy("date").get()
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
