package vcmsa.projects.crechemanagementapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.PARENT,
    val profileImageUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class UserRole {
    ADMIN, STAFF, PARENT
}