package vcmsa.projects.crechemanagementapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Child(
    val id: String = "",
    val name: String = "",
    val parentId: String = "",
    val teacherId: String = "",
    val dateOfBirth: String = "",
    val allergies: String = "",
    val medicalNotes: String = "",
    val emergencyContact: String = "",
    val profileImageUrl: String = "",
    val childId: String,
    val isActive: Boolean = true
) : Parcelable
/**
 * An enum class to define the different user roles.
 * Using an enum makes it easy to manage roles and avoid string typos.
 */
enum class Role {
    ADMIN,
    STAFF,
    PARENT
}