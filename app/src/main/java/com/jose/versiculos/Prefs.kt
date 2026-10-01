package com.jose.versiculos

import android.content.Context

/** Acceso sencillo a los ajustes guardados. */
class Prefs(ctx: Context) {

    private val sp = ctx.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean(K_ENABLED, true)
        set(v) = sp.edit().putBoolean(K_ENABLED, v).apply()

    var versionId: String
        get() = sp.getString(K_VERSION, "rvr1909") ?: "rvr1909"
        set(v) = sp.edit().putString(K_VERSION, v).apply()

    /** true = aleatorio, false = secuencial */
    var modeRandom: Boolean
        get() = sp.getBoolean(K_RANDOM, true)
        set(v) = sp.edit().putBoolean(K_RANDOM, v).apply()

    /** Minutos mínimos entre versículos (0 = cada desbloqueo). */
    var cooldownMinutes: Int
        get() = sp.getInt(K_COOLDOWN, 0)
        set(v) = sp.edit().putInt(K_COOLDOWN, v).apply()

    /** Tema de fondo: amanecer | nocturno | oceano | bosque */
    var theme: String
        get() = sp.getString(K_THEME, "amanecer") ?: "amanecer"
        set(v) = sp.edit().putString(K_THEME, v).apply()

    /** Categoría filtrada; vacío = todas. */
    var tag: String
        get() = sp.getString(K_TAG, "") ?: ""
        set(v) = sp.edit().putString(K_TAG, v).apply()

    var seqIndex: Int
        get() = sp.getInt(K_SEQ, -1)
        set(v) = sp.edit().putInt(K_SEQ, v).apply()

    var lastRef: String
        get() = sp.getString(K_LAST_REF, "") ?: ""
        set(v) = sp.edit().putString(K_LAST_REF, v).apply()

    var lastShownAt: Long
        get() = sp.getLong(K_LAST_SHOWN, 0L)
        set(v) = sp.edit().putLong(K_LAST_SHOWN, v).apply()

    var favorites: MutableSet<String>
        get() = sp.getStringSet(K_FAVORITES, mutableSetOf()) ?: mutableSetOf()
        set(v) = sp.edit().putStringSet(K_FAVORITES, v.toSet()).apply()

    companion object {
        private const val K_ENABLED = "enabled"
        private const val K_VERSION = "versionId"
        private const val K_RANDOM = "modeRandom"
        private const val K_COOLDOWN = "cooldownMinutes"
        private const val K_THEME = "theme"
        private const val K_TAG = "tag"
        private const val K_SEQ = "seqIndex"
        private const val K_LAST_REF = "lastRef"
        private const val K_LAST_SHOWN = "lastShownAt"
        private const val K_FAVORITES = "favorites"
    }
}
