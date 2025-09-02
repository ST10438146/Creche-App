package vcmsa.projects.crechemanagementapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.crechemanagementapp.databinding.ItemUserCardBinding


/**
 * RecyclerView adapter for displaying a list of users in the admin dashboard.
 *
 * This adapter handles the display of user information and allows the admin to
 * change a user's role via a Spinner.
 *
 * @param context The context for creating the spinner adapter.
 * @param onRoleUpdate A lambda function to be called when a user's role is updated.
 * It receives the User object and the new UserRole.
 */
class UsersAdapter(
    private val context: Context,
    private var users: MutableList<User>,
    private val onRoleUpdate: (User, UserRole) -> Unit
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemUserCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.userNameTextView.text = user.name
            binding.userEmailTextView.text = user.email

            val roleAdapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_item,
                UserRole.values().map { it.name }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            binding.roleSpinner.adapter = roleAdapter
            binding.roleSpinner.setSelection(UserRole.valueOf(user.role.uppercase()).ordinal)

            binding.roleSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        val newRole = UserRole.values()[position]
                        if (newRole.name != user.role.uppercase()) {
                            onRoleUpdate(user, newRole)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    // Helper to refresh list
    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}

