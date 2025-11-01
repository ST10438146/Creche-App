package vcmsa.projects.crechemanagementapp


data class Message(
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val content: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)
