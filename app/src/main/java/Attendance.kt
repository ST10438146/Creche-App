package vcmsa.projects.crechemanagementapp

data class Attendance(
    val id: String = "",
    val userId: String = "",
    val childId: String = "", // If tracking attendance per child
    val date: String = "", // Format "yyyy-MM-dd"
    val checkInTime: Long? = null,
    val checkOutTime: Long? = null,
    val status: String = "Not Checked In" // "Checked In", "Checked Out"
)
