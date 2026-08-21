package com.example.viewmodel

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IntruderLog
import com.example.data.SecurityPrefs
import com.example.receiver.MyDeviceAdminReceiver
import com.example.service.CameraForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SecurityUiState(
    val email: String = "",
    val password: String = "",
    val isTrackingEnabled: Boolean = false,
    val isAdminActive: Boolean = false,
    val hasCameraPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val logs: List<IntruderLog> = emptyList(),
    val isTesting: Boolean = false,
    val saveFeedback: Boolean = false,
    val bannerMessage: String? = null
)

class SecurityViewModel(private val context: Context) : ViewModel() {

    private val prefs = SecurityPrefs.getInstance(context)

    private val _uiState = MutableStateFlow(
        SecurityUiState(
            email = prefs.email,
            password = prefs.password,
            isTrackingEnabled = prefs.isTrackingEnabled,
            logs = prefs.getLogs()
        )
    )
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.trackingEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isTrackingEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            prefs.logsFlow.collect { logList ->
                _uiState.update { it.copy(logs = logList) }
            }
        }
        refreshStatuses()
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun saveCredentials() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password.trim()
        prefs.email = email
        prefs.password = password

        viewModelScope.launch {
            _uiState.update { it.copy(saveFeedback = true) }
            delay(2000)
            _uiState.update { it.copy(saveFeedback = false) }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        if (enabled && !_uiState.value.isAdminActive) {
            _uiState.update { it.copy(bannerMessage = "يرجى تفعيل صلاحية مدير الجهاز أولاً لتشغيل التتبع") }
            return
        }

        saveCredentials()
        prefs.isTrackingEnabled = enabled
        _uiState.update { it.copy(isTrackingEnabled = enabled) }

        val serviceIntent = Intent(context, CameraForegroundService::class.java).apply {
            action = if (enabled) CameraForegroundService.ACTION_START_MONITORING else CameraForegroundService.ACTION_STOP_MONITORING
        }

        try {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                context.stopService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun testAlert() {
        _uiState.update { it.copy(isTesting = true) }

        val serviceIntent = Intent(context, CameraForegroundService::class.java).apply {
            action = CameraForegroundService.ACTION_TEST_CAPTURE
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModelScope.launch {
            delay(4500)
            _uiState.update { it.copy(isTesting = false) }
        }
    }

    fun clearLogs() {
        prefs.clearLogs()
    }

    fun dismissBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    fun refreshStatuses() {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
        val isAdmin = dpm?.isAdminActive(adminComponent) == true

        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else {
            true
        }

        _uiState.update {
            it.copy(
                isAdminActive = isAdmin,
                hasCameraPermission = hasCamera,
                hasLocationPermission = hasLocation,
                isBatteryOptimizationIgnored = isBatteryIgnored
            )
        }
    }
}
