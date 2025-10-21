package vcmsa.projects.crechemanagementapp

import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPrefManager = SharedPrefManager.getInstance(this)

        initViews()
        loadSavedPreferences()
        setupSwitchListeners()
    }

    private fun initViews() {
        switchBiometric = findViewById(R.id.switchBiometric)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchNotifications = findViewById(R.id.switchNotifications)
    }

    private fun loadSavedPreferences() {
        switchBiometric.isChecked = sharedPrefManager.isBiometricEnabled()
        switchDarkMode.isChecked = sharedPrefManager.isDarkModeEnabled()
        switchNotifications.isChecked = sharedPrefManager.areNotificationsEnabled()
    }

    private fun setupSwitchListeners() {
        switchBiometric.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            sharedPrefManager.setBiometricEnabled(isChecked)
            val msg = if (isChecked) "Biometric login enabled" else "Biometric login disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            sharedPrefManager.setDarkModeEnabled(isChecked)
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        switchNotifications.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            sharedPrefManager.setNotificationsEnabled(isChecked)
            val msg = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
