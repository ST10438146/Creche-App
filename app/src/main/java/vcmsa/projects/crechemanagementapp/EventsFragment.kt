package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CalendarView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class EventsFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var rvEvents: RecyclerView
    private lateinit var btnAddEvent: Button // Declare the button
    private lateinit var firestoreDb: FirebaseFirestore
    private val eventsList = mutableListOf<Event>()
    private lateinit var eventAdapter: EventAdapter // Declared as lateinit

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
        // Load events for today initially
        val today = Calendar.getInstance()
        loadEventsForDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH))

        // Set up click listener for Add Event button
        btnAddEvent.setOnClickListener { // Now correctly initialized
            // Implement logic to open a dialog or new activity to add an event
            Toast.makeText(context, "Add Event (Not implemented yet)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews(view: View) {
        calendarView = view.findViewById(R.id.calendarView)
        rvEvents = view.findViewById(R.id.rvEvents)
        btnAddEvent = view.findViewById(R.id.btnAddEvent) // Initialize the button here
    }

    private fun setupCalendar() {
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Load events for selected date
            loadEventsForDate(year, month, dayOfMonth)
        }
    }

    private fun setupEventsList() {
        rvEvents.layoutManager = LinearLayoutManager(context)
        eventAdapter = EventAdapter(eventsList) // Initialize EventAdapter
        rvEvents.adapter = eventAdapter // Set EventAdapter to RecyclerView
    }

    /**
     * Loads events for the selected date from Firestore.
     */
    private fun loadEventsForDate(year: Int, month: Int, day: Int) {
        // Firebase months are 0-indexed, CalendarView months are also 0-indexed
        val selectedDate = Calendar.getInstance()
        selectedDate.set(year, month, day)
        val dateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)

        firestoreDb.collection("events")
            .whereEqualTo("date", dateFormatted)
            .orderBy("time", Query.Direction.ASCENDING) // Order by time for better display
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    eventsList.clear()
                    for (doc in snapshots.documents) {
                        val event = doc.toObject(Event::class.java)
                        event?.let { eventsList.add(it) }
                    }
                    eventAdapter.notifyDataSetChanged() // Notify EventAdapter of data changes

                    if (eventsList.isEmpty()) {
                        Toast.makeText(context, "No events for selected date.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}