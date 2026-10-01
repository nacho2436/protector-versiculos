# Protector de Versículos 📖✨

App Android nativa (Kotlin) que muestra **un versículo bíblico nuevo cada vez
que desbloqueas el teléfono**. La pantalla de ajustes permite elegir entre
distintas versiones de la Biblia (de dominio público), modo de selección,
frecuencia, categoría temática y fondo.

## Características

- **Versículo al desbloquear**: un servicio en primer plano detecta el
  desbloqueo (`ACTION_USER_PRESENT`) y abre una pantalla a pantalla completa
  con el versículo animado sobre un degradado. Se cierra tocando en cualquier
  parte, con el botón ✕, o atrás.
- **Versiones bíblicas**: incluye *Reina-Valera 1909* (español) y *King James
  Version* (inglés), ambas de dominio público. Añadir más versiones es tan
  fácil como copiar un JSON en `app/src/main/assets/versions/`.
- **Selección aleatoria o secuencial** (sin repetir).
- **Frecuencia**: cada desbloqueo, cada 15 min, cada hora o cada 3 horas.
- **Categorías**: ánimo, fe, amor, paz, fortaleza, gracia, gratitud,
  sabiduría, esperanza, salvación, alabanza, luz, oración, gozo.
- **Fondos**: Amanecer, Nocturno, Océano y Bosque.
- **Favoritos** (estrella ★) y **compartir** el versículo.
- Arranca sola tras reiniciar el teléfono (si estaba activada).

## Permisos

| Permiso | Motivo |
|---|---|
| `SYSTEM_ALERT_WINDOW` ("Mostrar sobre otras apps") | Necesario en Android 10+ para abrir la pantalla del versículo justo al desbloquear. Sin él, el versículo llega como **notificación** (modo degradado, funciona igual sin configurar nada). |
| `POST_NOTIFICATIONS` | Notificación discreta del servicio y del modo alternativo. |
| `FOREGROUND_SERVICE` + `_SPECIAL_USE` | Mantener activo el detector de desbloqueo. |
| `RECEIVE_BOOT_COMPLETED` | Reiniciar el servicio tras encender el teléfono. |

La app **no necesita internet**: todo funciona offline.

## Compilar

Requisitos: JDK 17, SDK de Android (platform 34, build-tools 34) y Gradle 8.7
(o usar el wrapper incluido).

```bash
./gradlew assembleRelease
# APK en app/build/outputs/apk/release/app-release.apk
```

La firma de release se configura con `keystore.properties` (en la raíz,
gitignored):

```properties
storeFile=versiculos-release.jks
storePassword=...
keyAlias=versiculos
keyPassword=...
```

## Añadir una versión bíblica nueva

Crea `app/src/main/assets/versions/miversion.json`:

```json
{
  "id": "miversion",
  "name": "Mi Versión",
  "language": "es",
  "verses": [
    { "ref": "Juan 3:16", "text": "...", "tags": ["amor", "fe"] }
  ]
}
```

Las `tags` disponibles están en `MainActivity.kt` (`tagLabels`). Recompila y la
versión aparece sola en el desplegable.

## Estructura

```
app/src/main/java/com/jose/versiculos/
├── App.kt             # Canales de notificación
├── MainActivity.kt    # Ajustes
├── VerseActivity.kt   # Pantalla del versículo
├── VerseService.kt    # Servicio primer plano + detector de desbloqueo
├── BootReceiver.kt    # Autoarranque tras reiniciar
├── Verses.kt          # Repositorio de versiones y versículos
└── Prefs.kt           # Preferencias
```

## Notas

- `minSdk 26` (Android 8.0+), `targetSdk 34` (Android 14).
- Icono adaptativo (cruz dorada + Biblia abierta + rayos sobre degradado
  índigo), con soporte de icono temático (Android 13+).
- Los textos bíblicos incluidos son ediciones de dominio público.
