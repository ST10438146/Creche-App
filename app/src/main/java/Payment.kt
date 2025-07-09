package vcmsa.projects.crechemanagementapp

data class Payment(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val description: String = "", // e.g., "Monthly Tuition", "Activity Fee"
    val date: Long = System.currentTimeMillis(), // Timestamp
    val status: String = "Paid" // "Paid", "Pending"
)
