package vcmsa.projects.crechemanagementapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties


@Parcelize
data class User(
    val uid: String = "",
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = UserRole.PARENT.name,
    val profileImageUrl: String = "",
    val isEnabled: Boolean = true, // For enabling/disabling user accounts
    val isActive: Boolean = true,
        val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class UserRole {
    ADMIN, STAFF, PARENT
}