package vcmsa.projects.crechemanagementapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class SharedPrefManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: User) {
        val json = gson.toJson(user)
        sharedPreferences.edit().putString(KEY_USER, json).apply()
    }

    fun getUser(): User? {
        val json = sharedPreferences.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clearUser() {
        sharedPreferences.edit().remove(KEY_USER).apply()
    }

    companion object {
        private const val PREF_NAME = "creche_prefs"
        private const val KEY_USER = "user_data"

        @Volatile
        private var instance: SharedPrefManager? = null

        fun getInstance(context: Context): SharedPrefManager {
            return instance ?: synchronized(this) {
                instance ?: SharedPrefManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
