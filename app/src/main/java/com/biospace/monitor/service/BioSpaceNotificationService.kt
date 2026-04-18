package com.biospace.monitor.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.biospace.monitor.MainActivity
import com.biospace.monitor.R
import com.biospace.monitor.ble.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class BioSpaceNotificationService : Service() {

    companion object {
        const val CHANNEL_ID_PERSISTENT = "biospace_persistent"
        const val CHANNEL_ID_ALERTS     = "biospace_alerts"
        const val NOTIF_ID_PERSISTENT   = 1001
        const val NOTIF_ID_ALERT_BASE   = 2000

        fun createChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java)

                // Persistent foreground notification (silent)
                nm.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID_PERSISTENT,
                    "BioSpace Monitor",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background monitoring status"
                    setShowBadge(false)
                })

                // Alert notifications (high priority)
                nm.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "Health & Space Weather Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Biometric and space weather alerts"
                    enableVibration(true)
                })
            }
        }

        fun buildPersistentNotification(context: Context, status: String): Notification {
            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            return NotificationCompat.Builder(context, CHANNEL_ID_PERSISTENT)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("BioSpace Monitor")
                .setContentText(status)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .build()
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var watchRepository: WatchRepository
    private var alertCount = 0

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
        watchRepository = WatchRepository(this)

        startForeground(
            NOTIF_ID_PERSISTENT,
            buildPersistentNotification(this, "Monitoring active")
        )

        watchRepository.start()
        observeAlerts()
        observeEventMode()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        watchRepository.stop()
        scope.cancel()
    }

    // ── Alert observer ────────────────────────────────────────────────────────

    private fun observeAlerts() {
        scope.launch {
            watchRepository.alerts.collect { alert ->
                sendAlertNotification(alert)
                updatePersistentNotification("⚠ ${alert.message}")
            }
        }
    }

    private fun observeEventMode() {
        scope.launch {
            watchRepository.isEventMode.collect { eventMode ->
                val status = if (eventMode) "🌩 Space weather event — recording"
                             else "Monitoring active"
                updatePersistentNotification(status)
            }
        }
    }

    private fun sendAlertNotification(alert: WatchAlert) {
        val nm = getSystemService(NotificationManager::class.java)
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, alertCount, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val icon = when (alert.type) {
            AlertType.HR_HIGH, AlertType.HR_LOW -> R.drawable.ic_launcher_foreground
            AlertType.BP_HIGH                   -> R.drawable.ic_launcher_foreground
            AlertType.SPO2_LOW                  -> R.drawable.ic_launcher_foreground
            AlertType.KP_STORM, AlertType.BZ_SOUTHWARD,
            AlertType.SOLAR_FLARE, AlertType.CME_DETECTED -> R.drawable.ic_launcher_foreground
            else -> R.drawable.ic_launcher_foreground
        }

        val notif = NotificationCompat.Builder(this, CHANNEL_ID_ALERTS)
            .setSmallIcon(icon)
            .setContentTitle(alert.message)
            .setContentText(alert.value)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(NOTIF_ID_ALERT_BASE + alertCount++, notif)
    }

    private fun updatePersistentNotification(status: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID_PERSISTENT, buildPersistentNotification(this, status))
    }

    // ── Space weather feed (called by MainActivity/ViewModel) ─────────────────
    fun updateSpaceWeather(sw: com.biospace.monitor.model.SpaceWeatherState) {
        watchRepository.onSpaceWeatherUpdate(sw)
    }

    fun getRepository(): WatchRepository = watchRepository
}
