package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

class AdminAttendanceFragment : Fragment() {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: SimpleListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val progress = view.findViewById<View>(R.id.progressBar)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        adapter = SimpleListAdapter(emptyList(), binder = { map, holder ->
            val name = map["userName"] ?: map["userId"] ?: "Unknown"
            val date = map["date"] ?: "N/A"
            val inT = map["checkInTime"] ?: "-"
            val outT = map["checkOutTime"] ?: "-"
            holder.titleTv.text = "$name — $date"
            holder.subtitleTv.text = "In: $inT  Out: $outT"
        }, onClick = { map ->
            // optional: open attendance detail
        })
        recycler.adapter = adapter

        progress.visibility = View.VISIBLE
        firestore.collection("attendance")
            .orderBy("date")
            .get()
            .addOnSuccessListener { result ->
                progress.visibility = View.GONE
                val list = result.map { it.data.toMap() }
                adapter.update(list)
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
