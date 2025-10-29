package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

class UserAttendanceFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: SimpleListAdapter
    private var userId: String? = null

    companion object {
        fun newInstance(userId: String) = UserAttendanceFragment().apply {
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
            val date = map["date"] ?: "N/A"
            val inT = map["checkInTime"] ?: ""
            val outT = map["checkOutTime"] ?: ""
            holder.titleTv.text = date.toString()
            holder.subtitleTv.text = "In: $inT  Out: $outT"
        })
        recycler.adapter = adapter

        if (userId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Missing userId", Toast.LENGTH_SHORT).show()
            return
        }

        progress.visibility = View.VISIBLE
        firestore.collection("attendance")
            .whereEqualTo("userId", userId)
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
