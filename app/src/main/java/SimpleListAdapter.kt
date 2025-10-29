package vcmsa.projects.crechemanagementapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleListAdapter(
    private var items: List<Map<String, Any>>,
    private val binder: (map: Map<String, Any>, holder: ViewHolder) -> Unit,
    private val onClick: (map: Map<String, Any>) -> Unit = {}
) : RecyclerView.Adapter<SimpleListAdapter.ViewHolder>() {

    fun update(newItems: List<Map<String, Any>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_simple_card, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val map = items[position]
        binder(map, holder)
        holder.itemView.setOnClickListener { onClick(map) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTv: TextView = itemView.findViewById(R.id.titleTv)
        val subtitleTv: TextView = itemView.findViewById(R.id.subtitleTv)
    }
}
