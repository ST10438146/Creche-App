package vcmsa.projects.crechemanagementapp

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class EventsFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var rvEvents: RecyclerView
    private lateinit var btnAddEvent: Button
    private lateinit var firestoreDb: FirebaseFirestore
    private val eventsList = mutableListOf<Event>()
    private lateinit var eventAdapter: EventAdapter

    // Keep track of current selected date (so Add Event uses it)
    private var selectedYear = 0
    private var selectedMonth = 0 // 0-indexed
    private var selectedDay = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_events, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestoreDb = FirebaseFirestore.getInstance()
        initViews(view)
        setupCalendar()
        setupEventsList()

        // initialize selected date to today
        val today = Calendar.getInstance()
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)
        loadEventsForDate(selectedYear, selectedMonth, selectedDay)

        // Seed sample events once (Nov & Dec sample data) - runs only the first time
        ensureSampleEventsSeeded()

        // Diagnostic: show how many events the current user can read right now
        diagnosticShowEventCount()

        btnAddEvent.setOnClickListener {
            showAddEventDialog()
        }
    }

    private fun initViews(view: View) {
        calendarView = view.findViewById(R.id.calendarView)
        rvEvents = view.findViewById(R.id.rvEvents)
        btnAddEvent = view.findViewById(R.id.btnAddEvent)
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            loadEventsForDate(year, month, dayOfMonth)
        }
    }

    private fun setupEventsList() {
        rvEvents.layoutManager = LinearLayoutManager(context)
        eventAdapter = EventAdapter(eventsList) { event ->
            // Item click -> show details
            showEventDetailsDialog(event)
        }
        rvEvents.adapter = eventAdapter
    }

    private fun showEventDetailsDialog(event: Event) {
        val message = StringBuilder()
            .append("Title: ").append(event.title).append("\n\n")
            .append("Date: ").append(event.date).append("\n")
            .append("Time: ").append(event.time).append("\n")
            .append("Location: ").append(event.location).append("\n\n")
            .append("Description:\n").append(event.description)
            .toString()

        AlertDialog.Builder(requireContext())
            .setTitle(event.title.ifEmpty { "Event details" })
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showAddEventDialog() {
        val dlgView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_event, null)

        val etTitle = dlgView.findViewById<EditText>(R.id.etEventTitle)
        val etDescription = dlgView.findViewById<EditText>(R.id.etEventDescription)
        val etDate = dlgView.findViewById<EditText>(R.id.etEventDate)
        val etTime = dlgView.findViewById<EditText>(R.id.etEventTime)
        val etLocation = dlgView.findViewById<EditText>(R.id.etEventLocation)
        val btnPickTime = dlgView.findViewById<Button>(R.id.btnPickTime)

        // Pre-fill date with currently selected date
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth, selectedDay)
        val prefillDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        etDate.setText(prefillDate)

        // Time picker
        btnPickTime.setOnClickListener {
            val now = Calendar.getInstance()
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val minute = now.get(Calendar.MINUTE)
            val tpd = TimePickerDialog(requireContext(), { _, h, m ->
                val hh = if (h < 10) "0$h" else "$h"
                val mm = if (m < 10) "0$m" else "$m"
                etTime.setText("$hh:$mm")
            }, hour, minute, true)
            tpd.show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Event")
            .setView(dlgView)
            .setPositiveButton("Save", null) // override later to prevent auto-dismiss on validation fail
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveBtn.setOnClickListener {
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val dateStr = etDate.text.toString().trim()
                val timeStr = etTime.text.toString().trim()
                val location = etLocation.text.toString().trim()

                if (title.isEmpty()) {
                    etTitle.error = "Enter a title"
                    return@setOnClickListener
                }
                if (dateStr.isEmpty()) {
                    etDate.error = "Enter a date (yyyy-MM-dd)"
                    return@setOnClickListener
                }
                // Basic validations
                val timeToSave = if (timeStr.isEmpty()) "09:00" else timeStr // default if empty
                saveEventToFirestore(title, description, dateStr, timeToSave, location)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun saveEventToFirestore(
        title: String,
        description: String,
        date: String,
        time: String,
        location: String
    ) {
        val docRef = firestoreDb.collection("events").document()
        val event = Event(
            id = docRef.id,
            title = title,
            description = description,
            date = date,
            time = time,
            location = location,
            createdAt = System.currentTimeMillis()
        )

        docRef.set(event)
            .addOnSuccessListener {
                Toast.makeText(context, "Event saved", Toast.LENGTH_SHORT).show()
                // reload events for currently selected date
                loadEventsForDate(selectedYear, selectedMonth, selectedDay)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save event: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("EVENT_SAVE", "Failed to save event ${event.title}", e)
            }
    }

    /**
     * Loads events for the selected date from Firestore.
     */
    private fun loadEventsForDate(year: Int, month: Int, day: Int) {
        val selectedDate = Calendar.getInstance()
        selectedDate.set(year, month, day)
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)

        firestoreDb.collection("events")
            .whereEqualTo("date", dateFormatted)
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("EVENT_LOAD", "Error loading events for $dateFormatted", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    eventsList.clear()
                    for (doc in snapshots.documents) {
                        val event = doc.toObject(Event::class.java)
                        event?.let { eventsList.add(it) }
                    }
                    eventAdapter.notifyDataSetChanged()

                    if (eventsList.isEmpty()) {
                        val emptyToast = Toast.makeText(context, "No events for selected date.", Toast.LENGTH_SHORT)
                        emptyToast.show()
                    }
                }
            }
    }

    /**
     * Seed sample events for Nov & Dec 2025 once (uses SharedPreferences flag to avoid duplication).
     * This version will attempt seeding unconditionally (no role check) but only once per install.
     * It reports successes/failures via Logcat and Toasts so you can debug permissions.
     */
    private fun ensureSampleEventsSeeded() {
        val prefs = requireContext().getSharedPreferences("creche_prefs", Context.MODE_PRIVATE)
        val seeded = prefs.getBoolean("sample_events_seeded", false)
        if (!seeded) {
            seedSampleEventsForNovDec()
            prefs.edit().putBoolean("sample_events_seeded", true).apply()
        } else {
            Log.d("SEED", "Sample events already seeded (pref flag present).")
        }
    }

    private fun seedSampleEventsForNovDec() {
        // Sample events for November and December 2025
        val sampleEvents = listOf(
            Event("", "Parent-Teacher Meeting", "Discuss child progress and concerns.", "2025-11-05", "17:30", "Main Hall"),
            Event("", "School Health Screening", "Routine health checks (vision & hearing).", "2025-11-12", "09:00", "Health Room"),
            Event("", "Field Trip: Little Farm Visit", "Educational trip to nearby farm.", "2025-11-20", "08:30", "Little Farm"),
            Event("", "Arts & Crafts Day", "Creative art activities for all groups.", "2025-11-28", "10:00", "Arts Room"),
            Event("", "Toy Drive & Donation Sorting", "Bring gently used toys for charity.", "2025-12-02", "09:30", "Lobby"),
            Event("", "Year-End Concert Rehearsal", "Practice for the Christmas concert.", "2025-12-10", "14:00", "School Hall"),
            Event("", "Christmas Play (Parents Invited)", "Annual Christmas performance.", "2025-12-18", "10:00", "Main Hall"),
            Event("", "Mini Graduation Ceremony", "Graduation for final-year group.", "2025-12-20", "11:00", "Outdoor Stage"),
            Event("", "Water Safety Session", "Safety & water play rules for kids.", "2025-12-05", "09:00", "Pool Area"),
            Event("", "Gardening Day", "Planting and seasonal gardening activities.", "2025-11-15", "10:00", "Garden")
        )

        var successCount = 0
        var failureCount = 0
        val total = sampleEvents.size

        for (e in sampleEvents) {
            val docRef = firestoreDb.collection("events").document()
            val toSave = e.copy(id = docRef.id, createdAt = System.currentTimeMillis())
            docRef.set(toSave)
                .addOnSuccessListener {
                    successCount++
                    Log.d("SEED", "Seeded event '${toSave.title}' (id=${docRef.id})")
                    if (successCount + failureCount == total) {
                        Toast.makeText(context, "Seeding finished: $successCount succeeded, $failureCount failed", Toast.LENGTH_LONG).show()
                        Log.d("SEED", "Seeding finished: $successCount succeeded, $failureCount failed")
                    }
                }
                .addOnFailureListener { ex ->
                    failureCount++
                    Log.e("SEED", "Failed to seed event '${toSave.title}': ${ex.message}", ex)
                    if (successCount + failureCount == total) {
                        Toast.makeText(context, "Seeding finished: $successCount succeeded, $failureCount failed", Toast.LENGTH_LONG).show()
                        Log.d("SEED", "Seeding finished: $successCount succeeded, $failureCount failed")
                    }
                }
        }
    }

    /**
     * Diagnostic helper - counts how many events are readable by the current user.
     * Useful to quickly surface permission / visibility issues in development.
     */
    private fun diagnosticShowEventCount() {
        firestoreDb.collection("events").get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                Toast.makeText(context, "Events in DB: $count", Toast.LENGTH_SHORT).show()
                Log.d("DEBUG_EVENTS", "Events docs: ${snap.documents.map { it.id to it.data }}")
            }
            .addOnFailureListener { ex ->
                Toast.makeText(context, "Failed to read events: ${ex.message}", Toast.LENGTH_LONG).show()
                Log.e("DEBUG_EVENTS", "Failed to read events", ex)
            }
    }
}
