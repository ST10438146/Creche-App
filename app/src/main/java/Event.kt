package vcmsa.projects.crechemanagementapp

data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "", // Format "yyyy-MM-dd"
    val time: String = "", // Format "HH:mm"
    val location: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
