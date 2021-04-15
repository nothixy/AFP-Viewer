package org.flval.afpviewer

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager

class SettingActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        supportFragmentManager.beginTransaction().replace(R.id.settview,
            SettingsFragment()
        ).commit()
    }
    class SettingsFragment: PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                "loginMode" -> {
                    val accessCodeTextInput: EditTextPreference? = findPreference("accessCode")
                    val usernameTextInput: EditTextPreference? = findPreference("username")
                    val passwordTextInput: EditTextPreference? = findPreference("password")
                    when (sharedPreferences?.getString(key, "auto")) {
                        "anon" -> {
                            accessCodeTextInput?.isEnabled = false
                            usernameTextInput?.isEnabled = false
                            passwordTextInput?.isEnabled = false
                        }
                        "temp" -> {
                            accessCodeTextInput?.isEnabled = true
                            usernameTextInput?.isEnabled = false
                            passwordTextInput?.isEnabled = false
                        }
                        "perm" -> {
                            accessCodeTextInput?.isEnabled = false
                            usernameTextInput?.isEnabled = true
                            passwordTextInput?.isEnabled = true
                        }
                    }
                }
            }
        }
        override fun onDestroy() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            val accessCodeTextInput: EditTextPreference? = findPreference("accessCode")
            val usernameTextInput: EditTextPreference? = findPreference("username")
            val passwordTextInput: EditTextPreference? = findPreference("password")
            when (PreferenceManager.getDefaultSharedPreferences(this.context).getString("loginMode", "auto").toString()) {
                "anon" -> {
                    accessCodeTextInput?.isEnabled = false
                    usernameTextInput?.isEnabled = false
                    passwordTextInput?.isEnabled = false
                }
                "temp" -> {
                    accessCodeTextInput?.isEnabled = true
                    usernameTextInput?.isEnabled = false
                    passwordTextInput?.isEnabled = false
                }
                "perm" -> {
                    accessCodeTextInput?.isEnabled = false
                    usernameTextInput?.isEnabled = true
                    passwordTextInput?.isEnabled = true
                }
            }
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }
    }

}