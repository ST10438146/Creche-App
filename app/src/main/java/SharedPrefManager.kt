package vcmsa.projects.crechemanagementapp

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth // Import Firebase Auth
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Manages local preferences like first-time app launch.
 * User session management (login/logout status) is primarily handled by Firebase Authentication.
 */
class SharedPrefManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val gson = Gson() // Initialize Gson

    companion object {
        private const val PREF_NAME = "CrecheAppPrefs"
        private const val KEY_FIRST_TIME = "first_time"
        private const val KEY_USER_DATA = "user_data" // New key to store user JSON

        @Volatile
        private var INSTANCE: SharedPrefManager? = null

        fun getInstance(context: Context): SharedPrefManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPrefManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Checks if the user is currently logged in via Firebase Authentication.
     * @return True if a user is authenticated, false otherwise.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Sets the first-time app launch flag.
     * @param isFirstTime True if it's the first time, false otherwise.
     */
    fun isFirstTime(): Boolean = sharedPreferences.getBoolean(KEY_FIRST_TIME, true)

    /**
     * Updates the first-time app launch flag.
     */
    fun setFirstTime(isFirstTime: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, isFirstTime).apply()
    }

    /**
     * Saves the logged-in User object to SharedPreferences as a JSON string.
     * Call this after successful login and fetching user data from Firestore.
     */
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_USER_DATA, userJson).apply()
    }

    /**
     * Retrieves the User object from SharedPreferences.
     * @return The User object if found, null otherwise.
     */
    fun getUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: JsonSyntaxException) {
                // Handle cases where the stored JSON is malformed
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    /**
     * Logs out the user from Firebase Authentication and clears local user data.
     */
    fun logout() {
        firebaseAuth.signOut() // Sign out from Firebase
        clearUser() // Clear locally stored user data
    }

    /**
     * Clears the locally stored User object.
     */
    private fun clearUser() {
        sharedPreferences.edit().remove(KEY_USER_DATA).apply()
    }
}