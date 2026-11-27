package com.voidglass.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class Lang(val code: String, val name: String, val hello: String, val cont: String)

val LANGS = listOf(
    Lang("es", "Español", "Hola", "Continuar"),
    Lang("en", "English", "Hello", "Continue"),
    Lang("pt", "Português", "Olá", "Continuar"),
    Lang("fr", "Français", "Bonjour", "Continuer"),
    Lang("de", "Deutsch", "Hallo", "Weiter"),
    Lang("it", "Italiano", "Ciao", "Continua"),
    Lang("ja", "日本語", "こんにちは", "続ける"),
    Lang("ko", "한국어", "안녕하세요", "계속"),
    Lang("zh", "中文", "你好", "继续"),
    Lang("ru", "Русский", "Привет", "Продолжить"),
    Lang("ar", "العربية", "مرحبا", "متابعة"),
    Lang("hi", "हिन्दी", "नमस्ते", "जारी रखें"),
    Lang("tr", "Türkçe", "Merhaba", "Devam"),
    Lang("nl", "Nederlands", "Hallo", "Doorgaan"),
    Lang("pl", "Polski", "Cześć", "Dalej"),
)

data class Mod(
    val id: String, val group: String, val es: String, val en: String,
    val dEs: String, val dEn: String, val conflicts: List<String>,
    val needsRoot: Boolean, val needsXposed: Boolean
)

val MODS = listOf(
    Mod("glass", "sys", "Liquid glass del sistema", "System liquid glass",
        "Cristal líquido en ajustes rápidos, notificaciones, volumen y bloqueo",
        "Liquid glass on quick settings, notifications, volume and lockscreen", listOf("hyperos"), true, true),
    Mod("hyperos", "sys", "Estilo HyperOS", "HyperOS style",
        "Centro de control, notificaciones y ajustes al estilo HyperOS",
        "HyperOS-style control center, notifications and settings", listOf("glass"), true, true),
    Mod("clock", "lock", "Reloj animado (XMorph)", "Animated clock (XMorph)",
        "Relojes que se transforman cada minuto en la pantalla de bloqueo",
        "Clocks that morph every minute on the lockscreen", listOf("hyperclock"), true, true),
    Mod("hyperclock", "lock", "Reloj estilo HyperOS", "HyperOS-style clock",
        "Reloj de bloqueo con la composición de HyperOS",
        "Lockscreen clock with HyperOS composition", listOf("clock"), true, true),
    Mod("statusbar", "ui", "Barra de estado", "Status bar",
        "Iconos, reloj y batería personalizables",
        "Customizable icons, clock and battery", listOf(), true, true),
    Mod("icons", "ui", "Paquetes de iconos", "Icon packs",
        "Iconos del sistema y del launcher",
        "System and launcher icons", listOf(), true, true),
    Mod("anim", "ui", "Animaciones", "Animations",
        "Velocidad y transiciones del sistema",
        "System speed and transitions", listOf(), true, false),
    Mod("fonts", "ui", "Fuentes", "Fonts",
        "Cambia la tipografía del sistema",
        "Change the system typeface", listOf(), true, false),
)

val GROUPS = listOf(
    Triple("sys", "Sistema", "System"),
    Triple("lock", "Pantalla de bloqueo", "Lock screen"),
    Triple("ui", "Interfaz", "Interface"),
)

typealias G = (Dp) -> Modifier

data class Dlg(val msg: String, val action: (() -> Unit)? = null, val actionLabel: String = "")

class AppState(val prefs: SharedPreferences) {
    var langIdx by mutableStateOf(prefs.getInt("lang", 0))
    var theme by mutableStateOf(prefs.getInt("theme", 0))
    var blur by mutableStateOf(prefs.getFloat("blur", 4f))
    var lens by mutableStateOf(prefs.getFloat("lens", 16f))
    var tint by mutableStateOf(prefs.getFloat("tint", 0.5f))
    var haptics by mutableStateOf(prefs.getBoolean("haptics", true))
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false))
    var root by mutableStateOf<Boolean?>(null)
    var lsposed by mutableStateOf<Boolean?>(null)
    var dialog by mutableStateOf<Dlg?>(null)
    var showLangs by mutableStateOf(false)
    val mods = mutableStateMapOf<String, Boolean>().also { m ->
        MODS.forEach { m[it.id] = prefs.getBoolean("mod_" + it.id, false) }
    }
    val lang: Lang get() = LANGS[langIdx]
    fun tr(es: String, en: String) = if (lang.code == "es") es else en
    fun save() {
        val e = prefs.edit()
            .putInt("lang", langIdx).putInt("theme", theme)
            .putFloat("blur", blur).putFloat("lens", lens).putFloat("tint", tint)
            .putBoolean("haptics", haptics).putBoolean("onboarded", onboarded)
        mods.forEach { (k, v) -> e.putBoolean("mod_$k", v) }
        e.apply()
    }
}

val LocalApp = staticCompositionLocalOf<AppState> { error("no app") }
val LocalDark = staticCompositionLocalOf { true }
val LocalGlass = staticCompositionLocalOf<G> { { _ -> Modifier } }

fun tintFor(dark: Boolean, tint: Float): Color =
    if (dark) Color.White.copy(alpha = 0.05f + 0.16f * tint)
    else Color.White.copy(alpha = 0.22f + 0.5f * tint)

suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
    try { Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor() == 0 } catch (e: Exception) { false }
}

suspend fun checkLsposed(): Boolean = withContext(Dispatchers.IO) {
    try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "ls /data/adb/modules"))
        val out = p.inputStream.bufferedReader().readText().lowercase()
        p.waitFor()
        out.contains("lsposed") || out.contains("vector") || out.contains("xposed")
    } catch (e: Exception) { false }
}

suspend fun runRoot(cmd: String): Boolean = withContext(Dispatchers.IO) {
    try { Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor() == 0 } catch (e: Exception) { false }
}

data class PermItem(val id: String, val es: String, val en: String, val dEs: String, val dEn: String, val ok: Boolean)

fun permItems(ctx: Context, root: Boolean?): List<PermItem> = listOf(
    PermItem("root", "Acceso root", "Root access",
        "Necesario para aplicar cambios al sistema", "Required to apply system changes", root == true),
    PermItem("notif", "Notificaciones", "Notifications",
        "Avisos de estado y errores", "Status and error alerts",
        Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED),
    PermItem("write", "Modificar ajustes del sistema", "Modify system settings",
        "Brillo, fuentes y animaciones", "Brightness, fonts and animations", Settings.System.canWrite(ctx)),
    PermItem("overlay", "Mostrar sobre otras apps", "Display over other apps",
        "Vistas previas y controles flotantes", "Previews and floating controls", Settings.canDrawOverlays(ctx)),
    PermItem("battery", "Batería sin restricciones", "Unrestricted battery",
        "Evita que el sistema cierre VoidGlass", "Keeps the system from closing VoidGlass",
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)),
    PermItem("files", "Acceso a todos los archivos", "All files access",
        "Guardar y cargar presets y fuentes", "Save and load presets and fonts", Environment.isExternalStorageManager()),
)

fun openPerm(ctx: Context, id: String) {
    val uri = Uri.parse("package:" + ctx.packageName)
    val i = when (id) {
        "write" -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, uri)
        "overlay" -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
        "battery" -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri)
        "files" -> Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
    }
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { ctx.startActivity(i) } catch (e: Exception) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: Exception) { }
    }
}

@Composable
fun Tx(
    text: String, size: Int = 16, w: FontWeight = FontWeight.Light, a: Float = 1f, sp: Float = 0f,
    modifier: Modifier = Modifier, align: TextAlign? = null,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text, modifier = modifier, color = color.copy(alpha = a), fontSize = size.sp,
        fontWeight = w, letterSpacing = sp.sp, textAlign = align
    )
}

@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.94f else 1f, spring(0.45f, 380f), label = "bounce")
    return this.graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

@Composable
fun Appear(i: Int = 0, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(i * 70L); shown = true }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 8 }
    ) { content() }
}

@Composable
fun BlackHole(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "bh")
    val ang by t.animateFloat(
        -30f, -10f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "a"
    )
    val pulse by t.animateFloat(
        0.95f, 1.08f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p"
    )
    val glow = cs.primary
    val ring = cs.onSurface
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.2f
        drawCircle(
            Brush.radialGradient(listOf(glow.copy(alpha = 0.40f), Color.Transparent), c, r * 3f * pulse),
            r * 3f * pulse, c
        )
        rotate(ang, c) {
            drawOval(
                ring.copy(alpha = 0.85f), Offset(c.x - r * 1.6f, c.y - r * 0.42f),
                Size(r * 3.2f, r * 0.84f), style = Stroke(1.5.dp.toPx())
            )
        }
        drawCircle(Color.Black, r, c)
        drawCircle(ring.copy(alpha = 0.9f), r, c, style = Stroke(2.dp.toPx()))
        drawCircle(ring.copy(alpha = 0.18f), r * 1.3f, c, style = Stroke(1.dp.toPx()))
    }
}
