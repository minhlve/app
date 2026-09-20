package com.offlinemessenger.transport

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class MeshForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { startForeground(9, notification()); return START_STICKY }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification() = (getSystemService(NotificationManager::class.java).also {
        it.createNotificationChannel(NotificationChannel("mesh", "Offline mesh", NotificationManager.IMPORTANCE_LOW))
    }).let { android.app.Notification.Builder(this, "mesh").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("Offline mesh is active").setContentText("Discovering nearby Offline Messenger devices").build() }
}
