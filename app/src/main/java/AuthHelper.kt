package vcmsa.projects.crechemanagementapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A helper object for authentication-related utilities.
 *
 * This object provides a central place to check a user's role and to perform
 * other auth-related tasks. It's crucial for implementing the role-based logic
 * described in the solution guide.
 */
object AuthHelper {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Checks if the current authenticated user has a specific role.
     * This method fetches the role from Firestore.
     * @param callback A function that receives a boolean indicating if the role matches.
     */
    fun hasRole(role: UserRole, callback: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            callback(false)
            return
        }

        // Fetch user data from Firestore to get their role
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val userRoleString = documentSnapshot.getString("role")
                callback(userRoleString.equals(role.name, ignoreCase = true))
            }
            .addOnFailureListener {
                // Handle failure, e.g., user not found or no internet
                callback(false)
            }
    }

    /**
     * A more direct way to check the role, useful after the role has been loaded
     * into a local object (e.g., from SharedPrefs or a ViewModel).
     */
    fun isUserRole(userRole: UserRole, roleToCheck: UserRole): Boolean {
        return userRole == roleToCheck
    }
}