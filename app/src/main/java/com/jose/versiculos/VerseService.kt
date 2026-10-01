package com.jose.versiculos

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Servicio en primer plano (notificación discreta) que escucha el desbloqueo
 * del teléfono (ACTION_USER_PRESENT) y muestra un versículo nuevo.
 *
 * En Android 10+ las apps no pueden abrir ventanas desde segundo plano sin el
 * permiso SYSTEM_ALERT_WINDOW ("Mostrar sobre otras apps"). Si está concedido
 * se abre la pantalla del versículo; si no, el versículo llega como notificación.
 */
class VerseService : Service() {

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) onUnlock()
        }
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        ContextCompat.registerReceiver(
            this,
            unlockReceiver,
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun startInForeground() {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n: Notification = NotificationCompat.Builder(this, App.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(getString(R.string.notif_service_title))
            .setContentText(getString(R.string.notif_service_text))
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            ServiceCompat.startForeground(this, NOTIF_ID, n, 0)
        }
    }

    private fun onUnlock() {
        val prefs = Prefs(this)
        if (!prefs.enabled) {
            stopSelf()
            return
        }
        val now = System.currentTimeMillis()
        val cooldownMs = prefs.cooldownMinutes * 60_000L
        if (now - prefs.lastShownAt < maxOf(cooldownMs, 2_000L)) return
        if (VerseActivity.isShowing) return

        val verse = Verses.next(this, prefs) ?: return
        prefs.lastShownAt = now

        if (Settings.canDrawOverlays(this)) {
            VerseActivity.start(this, verse)
        } else {
            notifyVerse(verse)
        }
    }

    /** Alternativa sin permiso de superposición: el versículo llega como notificación. */
    private fun notifyVerse(verse: Verses.Verse) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val pi = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, App.CHANNEL_VERSE)
            .setSmallIcon(R.drawable.ic_stat_book)
            .setContentTitle(verse.ref)
            .setStyle(NotificationCompat.BigTextStyle().bigText(verse.text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(VERSE_NOTIF_ID, n)
    }

    companion object {
        const val NOTIF_ID = 10
        const val VERSE_NOTIF_ID = 11

        /** Arranca o detiene el servicio según el ajuste principal. */
        fun sync(ctx: Context) {
            val intent = Intent(ctx, VerseService::class.java)
            if (Prefs(ctx).enabled) {
                ContextCompat.startForegroundService(ctx, intent)
            } else {
                ctx.stopService(intent)
            }
        }
    }
}
