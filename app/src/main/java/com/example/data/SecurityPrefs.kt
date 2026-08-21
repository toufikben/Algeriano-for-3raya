package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class IntruderLog(
    val id: String,
    val timestamp: Long,
    val photoPath: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val emailSent: Boolean,
    val statusMessage: String
)

class SecurityPrefs private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _trackingEnabledFlow = MutableStateFlow(isTrackingEnabled)
    val trackingEnabledFlow: StateFlow<Boolean> = _trackingEnabledFlow.asStateFlow()

    private val _logsFlow = MutableStateFlow(getLogs())
    val logsFlow: StateFlow<List<IntruderLog>> = _logsFlow.asStateFlow()

    companion object {
        private const val PREF_NAME = "intruder_security_prefs"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_PASSWORD = "key_password"
        private const val KEY_TRACKING_ENABLED = "key_tracking_enabled"
        private const val KEY_TOTAL_ATTEMPTS = "key_total_attempts"
        private const val KEY_LAST_ATTEMPT_TIME = "key_last_attempt_time"
        private const val KEY_LAST_ATTEMPT_LOCATION = "key_last_attempt_location"
        private const val KEY_LOGS_JSON = "key_logs_json"
        private const val KEY_THRESHOLD = "key_failed_threshold"

        @Volatile
        private var INSTANCE: SecurityPrefs? = null

        fun getInstance(context: Context): SecurityPrefs {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityPrefs(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value.trim()).apply()

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PASSWORD, value.trim()).apply()

    var isTrackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_TRACKING_ENABLED, value).apply()
            _trackingEnabledFlow.value = value
        }

    var totalAttempts: Int
        get() = prefs.getInt(KEY_TOTAL_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_ATTEMPTS, value).apply()

    var lastAttemptTime: Long
        get() = prefs.getLong(KEY_LAST_ATTEMPT_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ATTEMPT_TIME, value).apply()

    var lastAttemptLocation: String
        get() = prefs.getString(KEY_LAST_ATTEMPT_LOCATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_ATTEMPT_LOCATION, value).apply()

    var failedThreshold: Int
        get() = prefs.getInt(KEY_THRESHOLD, 1)
        set(value) = prefs.edit().putInt(KEY_THRESHOLD, value).apply()

    fun addLog(log: IntruderLog) {
        val currentLogs = getLogs().toMutableList()
        currentLogs.add(0, log)
        // Keep max 50 logs
        val trimmed = if (currentLogs.size > 50) currentLogs.take(50) else currentLogs
        saveLogs(trimmed)
        totalAttempts += 1
        lastAttemptTime = log.timestamp
        if (log.latitude != null && log.longitude != null) {
            lastAttemptLocation = "${log.latitude}, ${log.longitude}"
        }
        _logsFlow.value = trimmed
    }

    fun getLogs(): List<IntruderLog> {
        val jsonString = prefs.getString(KEY_LOGS_JSON, "[]") ?: "[]"
        val list = mutableListOf<IntruderLog>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    IntruderLog(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        timestamp = obj.optLong("timestamp", 0L),
                        photoPath = obj.optString("photoPath").takeIf { it.isNotEmpty() },
                        latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                        longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null,
                        address = obj.optString("address").takeIf { it.isNotEmpty() },
                        emailSent = obj.optBoolean("emailSent", false),
                        statusMessage = obj.optString("statusMessage", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveLogs(logs: List<IntruderLog>) {
        try {
            val jsonArray = JSONArray()
            for (log in logs) {
                val obj = JSONObject().apply {
                    put("id", log.id)
                    put("timestamp", log.timestamp)
                    put("photoPath", log.photoPath ?: "")
                    if (log.latitude != null) put("latitude", log.latitude)
                    if (log.longitude != null) put("longitude", log.longitude)
                    put("address", log.address ?: "")
                    put("emailSent", log.emailSent)
                    put("statusMessage", log.statusMessage)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_LOGS_JSON, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearLogs() {
        // Optionally delete image files
        val logs = getLogs()
        for (log in logs) {
            log.photoPath?.let { path ->
                try {
                    val file = File(path)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
        saveLogs(emptyList())
        totalAttempts = 0
        _logsFlow.value = emptyList()
    }
}
