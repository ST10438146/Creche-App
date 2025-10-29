package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

// User detail activity: shows profile, logs, attendance, payments, events
class UserDetailActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        userId = intent.getStringExtra("USER_ID") ?: run {
            finish(); return
        }

        val nameTv = findViewById<TextView>(R.id.ud_name)
        val emailTv = findViewById<TextView>(R.id.ud_email)
        val roleTv = findViewById<TextView>(R.id.ud_role)

        val logsRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_logs)
        val attendanceRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_attendance)
        val paymentsRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_payments)
        val eventsRv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_events)

        logsRv.layoutManager = LinearLayoutManager(this)
        attendanceRv.layoutManager = LinearLayoutManager(this)
        paymentsRv.layoutManager = LinearLayoutManager(this)
        eventsRv.layoutManager = LinearLayoutManager(this)

        val logsAdapter = SimpleListAdapter(emptyList(), binder = { m, h ->
            val ts = m["timestamp"]
            val tsText = when (ts) {
                is Timestamp -> ts.toDate().toString()
                is Date -> ts.toString()
                is Number -> Date(ts.toLong()).toString()
                else -> ts?.toString() ?: ""
            }
            h.titleTv.text = "${m["type"] ?: "action"} - ${m["details"] ?: ""}"
            h.subtitleTv.text = tsText
        })
        val attAdapter = SimpleListAdapter(emptyList(), binder = { m, h ->
            h.titleTv.text = "${m["date"] ?: "date"}"
            h.subtitleTv.text = "In: ${m["checkInTime"] ?: "-"} Out: ${m["checkOutTime"] ?: "-"}"
        })
        val payAdapter = SimpleListAdapter(emptyList(), binder = { m, h ->
            h.titleTv.text = "Amount: ${m["amount"] ?: "0"}"
            h.subtitleTv.text = "Status: ${m["status"] ?: "N/A"} Date: ${m["date"] ?: ""}"
        })
        val evtAdapter = SimpleListAdapter(emptyList(), binder = { m, h ->
            h.titleTv.text = "${m["title"] ?: m["description"] ?: "Event"}"
            h.subtitleTv.text = "Date: ${m["date"] ?: ""}"
        })

        logsRv.adapter = logsAdapter
        attendanceRv.adapter = attAdapter
        paymentsRv.adapter = payAdapter
        eventsRv.adapter = evtAdapter

        // Load profile
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val data = doc.data
                nameTv.text = data?.get("name")?.toString() ?: "Unknown"
                emailTv.text = data?.get("email")?.toString() ?: ""
                roleTv.text = data?.get("role")?.toString() ?: ""
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Load activity logs WITHOUT requiring a composite index (client-side sort)
        firestore.collection("activity_logs")
            .whereEqualTo("userId", userId)
            .limit(200) // adjust as needed
            .get()
            .addOnSuccessListener { snapshot ->
                // Convert to list of maps and sort by timestamp descending on client
                val list = snapshot.mapNotNull { doc ->
                    val data = doc.data.toMap().toMutableMap()
                    // keep the doc id if you need it
                    data["__docId"] = doc.id
                    data
                }.sortedWith(compareByDescending { map ->
                    toEpochMillis(map["timestamp"])
                })

                logsAdapter.update(list)
            }
            .addOnFailureListener { e ->
                // show error so you know what's wrong in dev
                Toast.makeText(this, "Logs load error: ${e.message}", Toast.LENGTH_LONG).show()
            }

        // Attendance
        firestore.collection("attendance")
            .whereEqualTo("userId", userId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { res -> attAdapter.update(res.map { it.data.toMap() }) }
            .addOnFailureListener { e -> Toast.makeText(this, "Attendance error: ${e.message}", Toast.LENGTH_SHORT).show() }

        // Payments
        firestore.collection("payments")
            .whereEqualTo("userId", userId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { res -> payAdapter.update(res.map { it.data.toMap() }) }
            .addOnFailureListener { e -> Toast.makeText(this, "Payments error: ${e.message}", Toast.LENGTH_SHORT).show() }

        // Events: createdBy & attendees
        firestore.collection("events")
            .whereEqualTo("createdBy", userId)
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { resA ->
                val listA = resA.map { it.data.toMap() }
                firestore.collection("events")
                    .whereArrayContains("attendees", userId)
                    .get()
                    .addOnSuccessListener { resB ->
                        val listB = resB.map { it.data.toMap() }
                        val combined = (listA + listB).distinctBy { it["id"] ?: it["title"] ?: it["date"] }
                        evtAdapter.update(combined)
                    }.addOnFailureListener {
                        // fallback to createdBy list
                        evtAdapter.update(listA)
                    }
            }
            .addOnFailureListener { e -> Toast.makeText(this, "Events error: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    /**
     * Robust helper to convert different timestamp representations to epoch millis.
     * Handles:
     *  - com.google.firebase.Timestamp
     *  - java.util.Date
     *  - numeric seconds/millis (Long/Number)
     *  - common ISO-like or simple date strings (several formats)
     * Returns 0L if unable to parse.
     */
    private fun toEpochMillis(ts: Any?): Long {
        when (ts) {
            null -> return 0L
            is Timestamp -> return ts.toDate().time
            is Date -> return ts.time
            is Number -> {
                val num = ts.toLong()
                // heuristic: if it's 10 digits or less => seconds
                return if (num.toString().length <= 10) num * 1000L else num
            }
            is String -> {
                val s = ts.trim()
                // try java.time.Instant.parse if available (API 26+ or desugared)
                try {
                    val instant = java.time.Instant.parse(s)
                    return instant.toEpochMilli()
                } catch (_: Exception) {
                    // fallback to other formats
                }

                val patterns = arrayOf(
                    "yyyy-MM-dd'T'HH:mm:ssX",        // 2023-10-29T12:34:56Z or +02:00
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",    // with millis
                    "yyyy-MM-dd HH:mm:ss",           // 2023-10-29 12:34:56
                    "yyyy-MM-dd"                     // date only
                )

                for (pat in patterns) {
                    try {
                        val sdf = SimpleDateFormat(pat, Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        val d = sdf.parse(s)
                        if (d != null) return d.time
                    } catch (_: ParseException) {
                        // try next
                    } catch (_: Exception) {
                        // try next
                    }
                }

                // last resort: try numeric string as epoch seconds/millis
                try {
                    val num = s.toLong()
                    return if (s.length <= 10) num * 1000L else num
                } catch (_: Exception) {
                    // give up
                }

                return 0L
            }
            else -> return 0L
        }
    }
}
