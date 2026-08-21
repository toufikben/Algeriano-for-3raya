package com.example.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.SecurityPrefs
import com.example.service.CameraForegroundService

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.d("DeviceAdminReceiver", "Password attempt failed!")

        val prefs = SecurityPrefs.getInstance(context)
        if (prefs.isTrackingEnabled) {
            val serviceIntent = Intent(context, CameraForegroundService::class.java).apply {
                action = CameraForegroundService.ACTION_CAPTURE_AND_SEND
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("DeviceAdminReceiver", "Failed to start foreground service", e)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.d("DeviceAdminReceiver", "Password succeeded")
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("DeviceAdminReceiver", "Device Admin Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("DeviceAdminReceiver", "Device Admin Disabled")
        // If disabled, turn off tracking to avoid inconsistent UI
        val prefs = SecurityPrefs.getInstance(context)
        prefs.isTrackingEnabled = false
    }
}
