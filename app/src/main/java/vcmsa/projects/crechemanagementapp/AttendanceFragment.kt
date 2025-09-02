package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.core.content.ContextCompat // Required for getColor

class AttendanceFragment : Fragment() {

    private lateinit var tvDate: TextView
    private lateinit var tvAttendanceStatus: TextView
    private lateinit var btnCheckIn: Button
    private lateinit var btnCheckOut: Button
    private lateinit var rvAttendanceHistory: RecyclerView

    private lateinit var sharedPrefManager: SharedPrefManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDb: FirebaseFirestore

    private var currentAttendanceId: String? = null // To track the current day's attendance document
    private var currentUserId: String? = null
    private var currentChildId: String? = null // For parents, if attendance is per child

    private lateinit var attendanceAdapter: AttendanceAdapter
    private val attendanceList = mutableListOf<Attendance>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firestoreDb = FirebaseFirestore.getInstance()
        sharedPrefManager = SharedPrefManager.getInstance(requireContext()) // Initialize SharedPrefManager

        currentUserId = firebaseAuth.currentUser?.uid

        initViews(view)
        setupClickListeners()

        // Load attendance history first, which will also set initial UI state
        setupAttendanceHistory()

        // Handle parent-specific child ID loading after sharedPrefManager is initialized
        val currentUser = sharedPrefManager.getUser()
        if (currentUser?.role?.equals(UserRole.PARENT.name, ignoreCase = true) == true) {
            currentUserId?.let { parentId ->
                firestoreDb.collection("children")
                    .whereEqualTo("parentId", parentId)
                    .limit(1) // Assuming one child per parent for this simple attendance
                    .get()
                    .addOnSuccessListener { childrenSnapshot ->
                        if (!childrenSnapshot.isEmpty) {
                            currentChildId = childrenSnapshot.documents[0].id
                        } else {
                            Toast.makeText(context, "No child found for this parent.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Could not fetch child for attendance: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun initViews(view: View) {
        tvDate = view.findViewById(R.id.tvDate)
        tvAttendanceStatus = view.findViewById(R.id.tvAttendanceStatus)
        btnCheckIn = view.findViewById(R.id.btnCheckIn)
        btnCheckOut = view.findViewById(R.id.btnCheckOut)
        rvAttendanceHistory = view.findViewById(R.id.rvAttendanceHistory)
    }

    private fun setupClickListeners() {
        btnCheckIn.setOnClickListener {
            performCheckIn()
        }
        btnCheckOut.setOnClickListener {
            performCheckOut()
        }
    }

    /**
     * Updates the UI based on the current attendance status.
     * @param status The current attendance status (e.g., "Checked In", "Not Checked In").
     * @param checkInTime Optional: The check-in timestamp.
     * @param checkOutTime Optional: The check-out timestamp.
     */
    private fun updateUI(status: String, checkInTime: Long? = null, checkOutTime: Long? = null) {
        val currentDate = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())
        tvDate.text = currentDate
        tvAttendanceStatus.text = status

        // Use ContextCompat.getColor for color resource access
        val color = when (status) {
            "Checked In" -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            "Checked Out" -> ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
        }
        tvAttendanceStatus.setTextColor(color)

        btnCheckIn.isEnabled = (status == "Not Checked In" || status == "Checked Out")
        btnCheckOut.isEnabled = (status == "Checked In")

        // In a real UI, you might display check-in/out times in separate TextViews.
        // For simplicity, we're just updating the status text.
    }

    /**
     * Performs a check-in operation by creating/updating a Firestore attendance record.
     */
    private fun performCheckIn() {
        val userId = currentUserId ?: run {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val checkInTimestamp = System.currentTimeMillis()

        // Query for today's attendance record
        firestoreDb.collection("attendance")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", currentDateFormatted)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // No record for today, create a new one
                    val newAttendanceRef = firestoreDb.collection("attendance").document()
                    currentAttendanceId = newAttendanceRef.id

                    val newAttendance = Attendance(
                        id = newAttendanceRef.id,
                        userId = userId,
                        childId = currentChildId ?: "", // Assign child ID if applicable for parents
                        date = currentDateFormatted,
                        checkInTime = checkInTimestamp,
                        status = "Checked In"
                    )

                    newAttendanceRef.set(newAttendance)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Checked in successfully!", Toast.LENGTH_SHORT).show()
                            updateUI("Checked In", checkInTimestamp)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error checking in: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Record exists, update it if not already checked in
                    val doc = querySnapshot.documents[0]
                    if (doc.getString("status") == "Not Checked In" || doc.getString("status") == "Checked Out") {
                        currentAttendanceId = doc.id
                        doc.reference.update(
                            mapOf(
                                "checkInTime" to checkInTimestamp,
                                "checkOutTime" to null, // Clear checkout if re-checking in
                                "status" to "Checked In"
                            )
                        )
                            .addOnSuccessListener {
                                Toast.makeText(context, "Checked in successfully!", Toast.LENGTH_SHORT).show()
                                updateUI("Checked In", checkInTimestamp)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error updating check-in: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Already checked in today.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error querying attendance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Performs a check-out operation by updating the Firestore attendance record.
     */
    private fun performCheckOut() {
        val userId = currentUserId ?: run {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val checkOutTimestamp = System.currentTimeMillis()

        // Find today's attendance record
        firestoreDb.collection("attendance")
            .whereEqualTo("userId", userId)
            .whereEqualTo("date", currentDateFormatted)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val doc = querySnapshot.documents[0]
                    if (doc.getString("status") == "Checked In") {
                        currentAttendanceId = doc.id
                        doc.reference.update(
                            mapOf(
                                "checkOutTime" to checkOutTimestamp,
                                "status" to "Checked Out"
                            )
                        )
                            .addOnSuccessListener {
                                Toast.makeText(context, "Checked out successfully!", Toast.LENGTH_SHORT).show()
                                updateUI("Checked Out", doc.getLong("checkInTime"), checkOutTimestamp)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Error checking out: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Cannot check out. Not currently checked in.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "No check-in record found for today.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error querying attendance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Sets up the RecyclerView to display real-time attendance history from Firestore.
     */
    private fun setupAttendanceHistory() {
        rvAttendanceHistory.layoutManager = LinearLayoutManager(context)

        // Initialize adapter here
        attendanceAdapter = AttendanceAdapter(attendanceList)
        rvAttendanceHistory.adapter = attendanceAdapter

        val userId = currentUserId ?: return

        // Listen for real-time changes to attendance records for the current user
        firestoreDb.collection("attendance")
            .whereEqualTo("userId", userId)
            // .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING) // Order by date, requires index
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading attendance history: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    attendanceList.clear()
                    for (doc in snapshots.documents) {
                        val attendance = doc.toObject(Attendance::class.java)
                        attendance?.let {
                            attendanceList.add(it)
                        }
                    }

                    // Sort locally if not using Firestore orderBy (due to index requirement)
                    attendanceList.sortByDescending { it.date }

                    // Notify adapter of data changes
                    attendanceAdapter.notifyDataSetChanged()

                    // Update the current day's status based on the latest data
                    val currentDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayAttendance = attendanceList.find { it.date == currentDateFormatted }

                    if (todayAttendance != null) {
                        currentAttendanceId = todayAttendance.id // Update current attendance ID
                        updateUI(
                            todayAttendance.status,
                            todayAttendance.checkInTime,
                            todayAttendance.checkOutTime
                        )
                    } else {
                        currentAttendanceId = null // Reset if no record for today
                        updateUI("Not Checked In")
                    }
                }
            }
    }
}