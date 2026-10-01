package com.jose.versiculos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Arranca el servicio tras reiniciar el teléfono, si el usuario lo tenía activado. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs(context).enabled) {
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, VerseService::class.java))
            }
        }
    }
}
