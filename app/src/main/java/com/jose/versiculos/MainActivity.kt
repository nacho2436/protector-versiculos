package com.jose.versiculos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jose.versiculos.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshStatus() }

    private val temasIds = listOf("amanecer", "nocturno", "oceano", "bosque")
    private val frecMinutos = listOf(0, 15, 60, 180)

    private val tagLabels = mapOf(
        "animo" to "Ánimo", "fe" to "Fe", "amor" to "Amor", "paz" to "Paz",
        "fortaleza" to "Fortaleza", "gracia" to "Gracia", "gratitud" to "Gratitud",
        "sabiduria" to "Sabiduría", "esperanza" to "Esperanza", "salvacion" to "Salvación",
        "alabanza" to "Alabanza", "luz" to "Luz", "oracion" to "Oración", "gozo" to "Gozo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)

        setupPrincipal()
        setupPermiso()
        setupVersion()
        setupSeleccion()
        setupFrecuencia()
        setupTema()
        setupCategoria()
        setupBotones()

        refreshStatus()
        refreshPermCard()

        // Asegura que el servicio quede en el estado correcto.
        VerseService.sync(this)
    }

    override fun onResume() {
        super.onResume()
        refreshPermCard()
        refreshStatus()
    }

    private fun setupPrincipal() {
        b.swEnabled.isChecked = prefs.enabled
        b.swEnabled.setOnCheckedChangeListener { _, checked ->
            prefs.enabled = checked
            pedirPermisoNotificaciones()
            VerseService.sync(this)
            refreshStatus()
        }
    }

    private fun setupPermiso() {
        b.btnPerm.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun setupVersion() {
        val versiones = Verses.versions(this)
        val nombres = versiones.map { "${it.name} (${it.language})" }.toTypedArray()
        b.actVersion.setSimpleItems(nombres)
        val actual = versiones.indexOfFirst { it.id == prefs.versionId }.takeIf { it >= 0 } ?: 0
        b.actVersion.setText(nombres[actual], false)
        b.actVersion.setOnItemClickListener { _, _, pos, _ ->
            prefs.versionId = versiones[pos].id
            cargarCategorias()
            refreshStatus()
        }
    }

    private fun setupSeleccion() {
        if (prefs.modeRandom) b.rbRandom.isChecked = true else b.rbSequential.isChecked = true
        b.rgMode.setOnCheckedChangeListener { _, id ->
            prefs.modeRandom = id == R.id.rbRandom
        }
    }

    private fun setupFrecuencia() {
        val labels = resources.getStringArray(R.array.frecuencia_labels)
        b.actFrecuencia.setSimpleItems(labels)
        val pos = frecMinutos.indexOf(prefs.cooldownMinutes).takeIf { it >= 0 } ?: 0
        b.actFrecuencia.setText(labels[pos], false)
        b.actFrecuencia.setOnItemClickListener { _, _, pos, _ ->
            prefs.cooldownMinutes = frecMinutos[pos]
        }
    }

    private fun setupTema() {
        val labels = resources.getStringArray(R.array.tema_labels)
        b.actTema.setSimpleItems(labels)
        val pos = temasIds.indexOf(prefs.theme).takeIf { it >= 0 } ?: 0
        b.actTema.setText(labels[pos], false)
        b.actTema.setOnItemClickListener { _, _, pos, _ ->
            prefs.theme = temasIds[pos]
        }
    }

    private fun setupCategoria() = cargarCategorias()

    private fun cargarCategorias() {
        val versos = Verses.list(this, prefs.versionId)
        val tags = versos.flatMap { it.tags }.distinct().sorted()
        val labels = (listOf(getString(R.string.cat_all)) + tags.map { tagLabels[it] ?: it }).toTypedArray()
        b.actCategoria.setSimpleItems(labels)
        if (prefs.tag.isBlank()) {
            b.actCategoria.setText(labels[0], false)
        } else {
            val i = tags.indexOf(prefs.tag)
            if (i >= 0) b.actCategoria.setText(labels[i + 1], false)
            else { prefs.tag = ""; b.actCategoria.setText(labels[0], false) }
        }
        b.actCategoria.setOnItemClickListener { _, _, pos, _ ->
            prefs.tag = if (pos == 0) "" else tags[pos - 1]
        }
    }

    private fun setupBotones() {
        b.btnProbar.setOnClickListener {
            val v = Verses.next(this, prefs)
            if (v == null) {
                Toast.makeText(this, R.string.error_sin_versiculos, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            VerseActivity.start(this, v)
        }
        b.btnFavoritos.setOnClickListener { mostrarFavoritos() }
    }

    private fun mostrarFavoritos() {
        val favs = prefs.favorites.toList().sorted()
        if (favs.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.btn_favoritos)
                .setMessage(R.string.favorites_empty)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val refs = favs.map { it.substringBefore("|") }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_favoritos)
            .setItems(refs) { _, which ->
                val entry = favs[which]
                val ref = entry.substringBefore("|")
                val text = entry.substringAfter("|")
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "«$text»\n— $ref")
                }
                startActivity(Intent.createChooser(send, getString(R.string.share_via)))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun refreshPermCard() {
        b.cardPerm.visibility = if (Settings.canDrawOverlays(this)) View.GONE else View.VISIBLE
    }

    private fun refreshStatus() {
        val nombreVersion = Verses.versionName(this, prefs.versionId)
        b.tvStatus.text = when {
            !prefs.enabled -> getString(R.string.status_inactive)
            !Settings.canDrawOverlays(this) ->
                getString(R.string.status_active_notif, nombreVersion)
            else -> getString(R.string.status_active, nombreVersion)
        }
    }
}
