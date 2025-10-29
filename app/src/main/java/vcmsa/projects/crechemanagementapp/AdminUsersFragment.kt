package vcmsa.projects.crechemanagementapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore

class AdminUsersFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var adapter: SimpleListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerView)
        val progress = view.findViewById<View>(R.id.progressBar)
        recycler.layoutManager = LinearLayoutManager(requireContext())

        adapter = SimpleListAdapter(emptyList(), binder = { map, holder ->
            val name = map["name"] ?: map["email"] ?: "Unknown"
            holder.titleTv.text = name.toString()
            holder.subtitleTv.text = "Role: ${map["role"] ?: "N/A"}"
        }, onClick = { map ->
            val userId = map["uid"]?.toString() ?: map["userId"]?.toString()
            if (!userId.isNullOrBlank()) {
                val intent = Intent(requireContext(), UserDetailActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Missing user id", Toast.LENGTH_SHORT).show()
            }
        })

        recycler.adapter = adapter

        progress.visibility = View.VISIBLE
        firestore.collection("users").get()
            .addOnSuccessListener { result ->
                progress.visibility = View.GONE
                val userList = result.map { it.data.toMap() }
                adapter.update(userList)
            }
            .addOnFailureListener { e ->
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
