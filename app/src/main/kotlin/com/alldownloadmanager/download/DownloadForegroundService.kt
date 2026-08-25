package com.alldownloadmanager.download
import android.app.*; import android.content.*; import android.os.IBinder; import androidx.core.app.NotificationCompat

class DownloadForegroundService : Service() {
    override fun onCreate() { super.onCreate(); val channel = NotificationChannel("downloads", "Active downloads", NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager::class.java).createNotificationChannel(channel); startForeground(7, notification("Preparing downloads")) }
    private fun notification(text: String) = NotificationCompat.Builder(this, "downloads").setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle("All Download Manager").setContentText(text).setOngoing(true).setCategory(NotificationCompat.CATEGORY_PROGRESS).build()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}