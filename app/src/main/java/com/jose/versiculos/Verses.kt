package com.jose.versiculos

import android.content.Context
import org.json.JSONObject

/**
 * Repositorio de versículos. Las versiones bíblicas son archivos JSON en
 * assets/versions/ con este formato:
 *
 * {
 *   "id": "rvr1909",
 *   "name": "Reina-Valera 1909",
 *   "language": "es",
 *   "verses": [
 *     { "ref": "Juan 3:16", "text": "...", "tags": ["amor", "fe"] }
 *   ]
 * }
 *
 * Para añadir una versión nueva basta con copiar un JSON más en esa carpeta
 * y volver a compilar; la app la detecta sola.
 */
object Verses {

    data class Verse(val ref: String, val text: String, val tags: List<String>) {
        val favoriteKey: String get() = "$ref|$text"
    }

    data class VersionInfo(val id: String, val name: String, val language: String, val file: String)

    private val cacheVersions = mutableListOf<VersionInfo>()
    private val cacheVerses = mutableMapOf<String, List<Verse>>()

    fun versions(ctx: Context): List<VersionInfo> {
        if (cacheVersions.isNotEmpty()) return cacheVersions.toList()
        val am = ctx.assets
        am.list("versions")
            ?.filter { it.endsWith(".json") }
            ?.sorted()
            ?.forEach { file ->
                runCatching {
                    val o = JSONObject(am.open("versions/$file").bufferedReader().use { it.readText() })
                    cacheVersions.add(
                        VersionInfo(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            language = o.optString("language"),
                            file = file
                        )
                    )
                }
            }
        return cacheVersions.toList()
    }

    fun list(ctx: Context, versionId: String): List<Verse> {
        val versions = versions(ctx)
        val target = versions.firstOrNull { it.id == versionId } ?: versions.firstOrNull() ?: return emptyList()
        cacheVerses[target.id]?.let { return it }
        val txt = ctx.assets.open("versions/${target.file}").bufferedReader().use { it.readText() }
        val arr = JSONObject(txt).optJSONArray("verses") ?: return emptyList()
        val out = ArrayList<Verse>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val tags = ArrayList<String>()
            o.optJSONArray("tags")?.let { t ->
                for (j in 0 until t.length()) tags.add(t.optString(j))
            }
            out.add(Verse(o.optString("ref"), o.optString("text"), tags))
        }
        cacheVerses[target.id] = out
        return out
    }

    fun versionName(ctx: Context, versionId: String): String =
        versions(ctx).firstOrNull { it.id == versionId }?.name ?: versionId

    /** Devuelve el siguiente versículo según modo, categoría y versión elegidos. */
    fun next(ctx: Context, prefs: Prefs): Verse? {
        var pool = list(ctx, prefs.versionId)
        if (prefs.tag.isNotBlank()) pool = pool.filter { prefs.tag in it.tags }
        if (pool.isEmpty()) return null
        return if (prefs.modeRandom) {
            var v: Verse
            do {
                v = pool.random()
            } while (pool.size > 1 && v.ref == prefs.lastRef)
            prefs.lastRef = v.ref
            v
        } else {
            val idx = (prefs.seqIndex + 1).mod(pool.size)
            prefs.seqIndex = idx
            val v = pool[idx]
            prefs.lastRef = v.ref
            v
        }
    }
}
