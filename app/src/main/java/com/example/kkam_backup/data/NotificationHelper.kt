package com.example.kkam_backup.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.kkam_backup.R

class NotificationHelper(private val context: Context) {

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun sendAnomalyAlert() {
        val channelId = "anomaly_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "이상 행동 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "무인 매장 이상 행동 알림 채널"
            }
            manager.createNotificationChannel(channel)
        }

        // Builder.apply { … } 로 묶어 주면 메서드 체이닝이 훨씬 깔끔해집니다
        val notification = NotificationCompat.Builder(context, channelId)
            .apply {
                // Android 내장 경고 아이콘 사용
                setSmallIcon(android.R.drawable.ic_dialog_alert)
                setContentTitle("이상 행동 감지")
                setContentText("매장에서 이상 행동이 감지되었습니다.")
                priority = NotificationCompat.PRIORITY_HIGH
                setAutoCancel(true)
            }
            .build()


        manager.notify(1001, notification)
    }
}
