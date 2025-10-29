// Utils.kt
package vcmsa.projects.crechemanagementapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

object ActivityLogger {
    private val firestore = FirebaseFirestore.getInstance()

    fun log(userId: String, type: String, details: String = "") {
        val doc = hashMapOf(
            "userId" to userId,
            "type" to type,
            "details" to details,
            "timestamp" to Timestamp.now()
        )
        firestore.collection("activity_logs").add(doc)
        // failure handling optional
    }
}
