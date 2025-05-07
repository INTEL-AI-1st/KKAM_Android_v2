package com.example.kkam_backup.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.kkam_backup.R

object NotificationHelper {

    const val CHANNEL_ID = "alert_channel"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)!!

        // 1) 사운드 URI 준비
        val soundUri = Uri.parse("android.resource://${context.packageName}/raw/alert_sound")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 2) 채널 생성 및 사운드/진동 설정
        val channel = NotificationChannel(
            CHANNEL_ID,
            "이상행동 알림",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(soundUri, attrs)
            enableVibration(true)
        }

        manager.createNotificationChannel(channel)
    }

    fun showHeadsUp(context: Context, id: Int, title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // ← use the existing bell icon you have in your drawables
            .setSmallIcon(R.drawable.ic_store_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}
