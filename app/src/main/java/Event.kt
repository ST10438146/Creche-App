package vcmsa.projects.crechemanagementapp

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Event(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var date: String = "",   // "yyyy-MM-dd"
    var time: String = "",   // "HH:mm"
    var location: String = "",
    var createdAt: Long = System.currentTimeMillis()
)
