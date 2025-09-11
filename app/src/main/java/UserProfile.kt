package vcmsa.projects.crechemanagementapp

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

// A data class to represent a user's profile in Firestore.
// The @IgnoreExtraProperties annotation is a good practice to prevent crashes
// if we add more properties to the Firestore document later.
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val email: String? = "",
    val role: String = "", // Can be "admin", "staff", or "parent"
    var fullName: String = "",
    var phoneNumber: String = "",
    var address: String = "",
    // Parent-specific fields
    var childName: String = "",
    var childAge: Int = 0,
    var childAllergies: String = "",
    // Staff-specific fields
    var staffId: String = ""
) {
    // Exclude the ID from being written to Firestore as a field
    @Exclude
    @set:Exclude
    var id: String? = null
}