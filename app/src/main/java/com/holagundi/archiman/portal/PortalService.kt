package com.holagundi.archiman.portal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.holagundi.archiman.MainActivity
import com.holagundi.archiman.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PortalService : Service() {
    @Inject lateinit var portal: PortalServer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            portal.stop(notify = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(null),
            if (android.os.Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        portal.onStopped = {
            Handler(Looper.getMainLooper()).post {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        val address = intent?.getStringExtra(ADDRESS)
        if (address.isNullOrBlank()) {
            if (!portal.isRunning()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                publish(portal.state.value.address)
            }
            return START_NOT_STICKY
        }
        // TLS keystore work and bind can take hundreds of ms on StrongBox devices.
        Thread({
            if (!portal.start(address)) {
                Handler(Looper.getMainLooper()).post {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    publish(portal.state.value.address)
                }
            }
        }, "archiman-portal-start").apply {
            isDaemon = true
            start()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        portal.onStopped = null
        if (portal.isRunning()) portal.stop(notify = false)
        super.onDestroy()
    }

    private fun publish(url: String?) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(url))
    }

    private fun notification(url: String?): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, getString(R.string.portal_channel), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.portal_notification)
                    setShowBadge(false)
                },
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, PortalService::class.java).setAction(STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = if (url.isNullOrBlank()) {
            getString(R.string.portal_notification)
        } else {
            getString(R.string.portal_notification_url, url)
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle(getString(R.string.portal_on))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, getString(R.string.portal_stop), stop)
            .build()
    }

    companion object {
        const val ADDRESS = "address"
        const val STOP = "com.holagundi.archiman.portal.STOP"
        private const val CHANNEL = "archiman_portal"
        private const val NOTIFICATION_ID = 41
    }
}
