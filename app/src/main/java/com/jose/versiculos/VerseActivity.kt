package com.jose.versiculos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jose.versiculos.databinding.ActivityVerseBinding

/** Pantalla a pantalla completa que muestra el versículo tras el desbloqueo. */
class VerseActivity : AppCompatActivity() {

    private lateinit var b: ActivityVerseBinding
    private lateinit var verse: Verses.Verse
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isShowing = true
        b = ActivityVerseBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        val ref = intent.getStringExtra(EXTRA_REF)
        val text = intent.getStringExtra(EXTRA_TEXT)
        verse = if (ref != null && text != null) {
            Verses.Verse(ref, text, emptyList())
        } else {
            Verses.next(this, prefs) ?: Verses.Verse(
                getString(R.string.fallback_ref),
                getString(R.string.fallback_text),
                emptyList()
            )
        }

        // Inmersión: sin barras de sistema.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, b.root).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        overridePendingTransition(R.anim.fade_in, 0)

        b.root.setBackgroundResource(themeBackground(prefs.theme))
        b.verseText.text = "«${verse.text}»"
        b.refText.text = getString(
            R.string.verse_ref_format,
            verse.ref.uppercase(),
            Verses.versionName(this, prefs.versionId).uppercase()
        )

        b.btnClose.setOnClickListener { cerrar() }
        b.btnShare.setOnClickListener { compartir() }
        b.btnStar.setOnClickListener { alternarFavorito() }
        b.root.setOnClickListener { cerrar() }
        b.scroll.setOnClickListener { cerrar() }
        renderStar()

        // Animación de entrada suave.
        b.verseText.alpha = 0f
        b.refText.alpha = 0f
        b.verseText.translationY = 40f
        b.refText.translationY = 40f
        b.verseText.animate().alpha(1f).translationY(0f)
            .setDuration(600).setInterpolator(OvershootInterpolator(1.1f)).start()
        b.refText.animate().alpha(1f).translationY(0f).setStartDelay(150).setDuration(600).start()
    }

    override fun onDestroy() {
        isShowing = false
        super.onDestroy()
    }

    private fun cerrar() {
        finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    private fun compartir() {
        val nombre = Verses.versionName(this, prefs.versionId)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "«${verse.text}»\n— ${verse.ref} ($nombre)")
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_via)))
    }

    private fun alternarFavorito() {
        val favs = prefs.favorites.toMutableSet()
        if (favs.contains(verse.favoriteKey)) {
            favs.remove(verse.favoriteKey)
            Toast.makeText(this, R.string.fav_removed, Toast.LENGTH_SHORT).show()
        } else {
            favs.add(verse.favoriteKey)
            Toast.makeText(this, R.string.fav_added, Toast.LENGTH_SHORT).show()
        }
        prefs.favorites = favs
        renderStar()
    }

    private fun renderStar() {
        val fav = prefs.favorites.contains(verse.favoriteKey)
        b.btnStar.setImageResource(if (fav) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
    }

    private fun themeBackground(theme: String): Int = when (theme) {
        "nocturno" -> R.drawable.bg_nocturno
        "oceano" -> R.drawable.bg_oceano
        "bosque" -> R.drawable.bg_bosque
        else -> R.drawable.bg_amanecer
    }

    companion object {
        var isShowing = false
        private const val EXTRA_REF = "ref"
        private const val EXTRA_TEXT = "text"

        fun start(ctx: Context, verse: Verses.Verse) {
            ctx.startActivity(
                Intent(ctx, VerseActivity::class.java)
                    .putExtra(EXTRA_REF, verse.ref)
                    .putExtra(EXTRA_TEXT, verse.text)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
