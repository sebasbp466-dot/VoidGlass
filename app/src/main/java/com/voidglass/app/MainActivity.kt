package com.voidglass.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val id: String, val es: String, val en: String,
    val descEs: String, val descEn: String, val conflicts: List<String>
)

val MODS = listOf(
    Mod("glass", "Liquid glass del sistema", "System liquid glass",
        "Cristal líquido en ajustes rápidos, notificaciones y bloqueo",
        "Liquid glass on quick settings, notifications and lockscreen", listOf("hyperos")),
    Mod("clock", "Reloj de bloqueo", "Lockscreen clock",
        "Relojes animados para la pantalla de bloqueo",
        "Animated lockscreen clocks", listOf()),
    Mod("hyperos", "Estilo HyperOS", "HyperOS style",
        "Centro de control y ajustes al estilo HyperOS",
        "HyperOS-style control center and settings", listOf("glass")),
)

typealias G = (Dp) -> Modifier

suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
    try { Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor() == 0 } catch (e: Exception) { false }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VoidApp() }
    }
}

@Composable
fun Tx(
    text: String, size: Int = 16, w: FontWeight = FontWeight.ExtraLight, a: Float = 1f,
    sp: Float = 0f, modifier: Modifier = Modifier, align: TextAlign? = null
) {
    Text(
        text, modifier = modifier, color = Color.White.copy(alpha = a), fontSize = size.sp,
        fontWeight = w, letterSpacing = sp.sp, textAlign = align
    )
}

@Composable
fun BlackHole(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.22f
        rotate(-20f, c) {
            drawOval(
                Color(0xFFCFCFE8),
                topLeft = Offset(c.x - r * 1.5f, c.y - r * 0.4f),
                size = Size(r * 3f, r * 0.8f),
                style = Stroke(1.5.dp.toPx())
            )
        }
        drawCircle(Color.Black, r, c)
        drawCircle(Color(0xFFF2F2FF), r, c, style = Stroke(2.dp.toPx()))
        drawCircle(Color(0x33FFFFFF), r * 1.3f, c, style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun Pill(text: String, glass: G, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).then(glass(26.dp)).clickable(onClick = onClick).padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) { Tx(text, 16, sp = 1f) }
}

@Composable
fun Card(glass: G, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().then(glass(24.dp)).padding(20.dp), content = content)
}

@Composable
fun MiniSwitch(on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.width(48.dp).height(28.dp).clip(CircleShape)
            .background(if (on) Color(0xFFEDEDF7) else Color(0x33FFFFFF))
            .clickable(onClick = onClick).padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.size(22.dp).background(if (on) Color.Black else Color.White, CircleShape))
    }
}

@Composable
fun Overlay(glass: G, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.padding(32.dp).fillMaxWidth().then(glass(28.dp)).clickable { }) { content() }
    }
}

@Composable
fun Welcome(lang: Lang, glass: G, onLang: () -> Unit, onNext: () -> Unit) {
    var intro by remember { mutableStateOf(true) }
    var hello by remember { mutableStateOf(LANGS[0].hello) }
    LaunchedEffect(Unit) {
        for (l in LANGS) { hello = l.hello; delay(500) }
        intro = false
    }
    LaunchedEffect(lang.code) { if (lang.code != "es") intro = false }
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Tx(lang.name, 14, a = 0.6f, sp = 1f, modifier = Modifier.clickable { onLang() }.padding(8.dp))
        }
        Spacer(Modifier.weight(1f))
        BlackHole(Modifier.size(110.dp))
        Spacer(Modifier.height(36.dp))
        Crossfade(if (intro) hello else lang.hello, animationSpec = tween(500), label = "h") {
            Tx(it, 40, FontWeight.Thin, sp = 2f)
        }
        Spacer(Modifier.weight(1f))
        Pill(lang.cont, glass, Modifier.fillMaxWidth(), onNext)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PermRow(name: String, desc: String, ok: Boolean, action: String, glass: G, onClick: () -> Unit) {
    Card(glass) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Tx(name, 18, FontWeight.Light)
                Spacer(Modifier.height(4.dp))
                Tx(desc, 13, a = 0.5f)
            }
            if (ok) Tx("✓", 20, a = 0.9f)
            else Tx(action, 14, FontWeight.Normal, a = 0.9f, sp = 1f, modifier = Modifier.clickable { onClick() }.padding(8.dp))
        }
    }
}

@Composable
fun Permissions(tr: (String, String) -> String, glass: G, root: Boolean?, onRoot: (Boolean) -> Unit, onNext: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var notif by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notif = it }
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Spacer(Modifier.height(24.dp))
        Tx(tr("Permisos", "Permissions"), 34, FontWeight.Thin, sp = 2f)
        Spacer(Modifier.height(8.dp))
        Tx(tr("Lo mínimo para empezar.", "Just what's needed to start."), 14, a = 0.5f)
        Spacer(Modifier.height(36.dp))
        PermRow(
            tr("Acceso root", "Root access"), tr("Para aplicar cambios al sistema", "To apply changes to the system"),
            root == true, tr("Conceder", "Grant"), glass
        ) { scope.launch { onRoot(checkRoot()) } }
        Spacer(Modifier.height(16.dp))
        PermRow(
            tr("Notificaciones", "Notifications"), tr("Avisos de estado y errores", "Status and error alerts"),
            notif, tr("Conceder", "Grant"), glass
        ) { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        Spacer(Modifier.weight(1f))
        Pill(tr("Continuar", "Continue"), glass, Modifier.fillMaxWidth(), onNext)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun CreditRow(name: String, project: String, desc: String) {
    Column(Modifier.padding(bottom = 22.dp)) {
        Tx(name, 18, FontWeight.Light)
        if (project.isNotEmpty()) Tx(project, 13, a = 0.55f, sp = 0.5f)
        Spacer(Modifier.height(2.dp))
        Tx(desc, 13, a = 0.4f)
    }
}

@Composable
fun Credits(tr: (String, String) -> String, glass: G, last: Boolean, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            Tx(tr("Gracias", "Thanks"), 34, FontWeight.Thin, sp = 2f)
            Spacer(Modifier.height(8.dp))
            Tx(tr("VoidGlass existe gracias al trabajo de:", "VoidGlass exists thanks to the work of:"), 14, a = 0.5f)
            Spacer(Modifier.height(32.dp))
            CreditRow("Kyant0", "AndroidLiquidGlass",
                tr("El efecto liquid glass de esta app", "The liquid glass effect of this app"))
            CreditRow("Mahmud0808 (DrDisagree)", "GlassLayer · XMorphClock",
                tr("Liquid glass de sistema y relojes de bloqueo", "System liquid glass and lockscreen clocks"))
            CreditRow("LSPosed", "Xposed",
                tr("La base para modificar el sistema", "The base for modifying the system"))
            CreditRow("LineageOS", "",
                tr("El sistema donde nace todo esto", "The system where all this is born"))
            Spacer(Modifier.height(10.dp))
            Card(glass) {
                Tx(tr("Beta temprana", "Early beta"), 16, FontWeight.Light, sp = 1f)
                Spacer(Modifier.height(6.dp))
                Tx(
                    tr(
                        "Probada solo en Redmi 9 (Lancelot) con LineageOS 24.0, Android 17. En otros dispositivos puede fallar.",
                        "Tested only on Redmi 9 (Lancelot) with LineageOS 24.0, Android 17. It may fail on other devices."
                    ), 13, a = 0.55f
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Pill(if (last) tr("Entrar", "Enter") else tr("Volver", "Back"), glass, Modifier.fillMaxWidth(), onDone)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun Home(
    tr: (String, String) -> String, glass: G, enabled: Map<String, Boolean>, root: Boolean?,
    langName: String, onToggle: (Mod) -> Unit, onLang: () -> Unit, onCredits: () -> Unit, onReplay: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (tab) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BlackHole(Modifier.size(110.dp)) }
                    Spacer(Modifier.height(16.dp))
                    Tx("VoidGlass", 34, FontWeight.Thin, sp = 3f, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
                    Tx("beta 0.1", 12, a = 0.45f, sp = 3f, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
                    Spacer(Modifier.height(36.dp))
                    Card(glass) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Tx("Root", 16, FontWeight.Light)
                            Tx(
                                when (root) { true -> tr("Activo", "Active"); false -> tr("No detectado", "Not detected"); else -> "…" },
                                15, a = 0.6f
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Tx(tr("Módulos activos", "Active modules"), 16, FontWeight.Light)
                            Tx("${MODS.count { enabled[it.id] == true }} / ${MODS.size}", 15, a = 0.6f)
                        }
                    }
                }
                1 -> {
                    Spacer(Modifier.height(16.dp))
                    Tx(tr("Módulos", "Modules"), 30, FontWeight.Thin, sp = 2f)
                    Spacer(Modifier.height(6.dp))
                    Tx(tr("Aún no aplican cambios reales al sistema.", "They don't apply real system changes yet."), 13, a = 0.45f)
                    Spacer(Modifier.height(24.dp))
                    MODS.forEach { m ->
                        Card(glass) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Tx(tr(m.es, m.en), 17, FontWeight.Light)
                                    Spacer(Modifier.height(4.dp))
                                    Tx(tr(m.descEs, m.descEn), 12, a = 0.45f)
                                }
                                MiniSwitch(enabled[m.id] == true) { onToggle(m) }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
                else -> {
                    Spacer(Modifier.height(16.dp))
                    Tx(tr("Ajustes", "Settings"), 30, FontWeight.Thin, sp = 2f)
                    Spacer(Modifier.height(24.dp))
                    @Composable
                    fun Row2(label: String, value: String, onClick: () -> Unit) {
                        Row(
                            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Tx(label, 17, FontWeight.Light)
                            Tx(value, 15, a = 0.45f)
                        }
                    }
                    Row2(tr("Idioma", "Language"), langName, onLang)
                    Row2(tr("Créditos", "Credits"), "", onCredits)
                    Row2(tr("Repetir bienvenida", "Replay welcome"), "", onReplay)
                    Row2(tr("Versión", "Version"), "0.1 beta") { }
                }
            }
        }
        Row(
            Modifier.padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth().height(60.dp).then(glass(30.dp)),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(tr("Inicio", "Home"), tr("Módulos", "Modules"), tr("Ajustes", "Settings")).forEachIndexed { i, s ->
                Tx(
                    s, 15, if (tab == i) FontWeight.Normal else FontWeight.ExtraLight,
                    a = if (tab == i) 1f else 0.5f, sp = 1f,
                    modifier = Modifier.clickable { tab = i }.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun VoidApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("voidglass", 0) }
    val backdrop = rememberLayerBackdrop()
    var langIdx by remember { mutableStateOf(prefs.getInt("lang", 0)) }
    val lang = LANGS[langIdx]
    val tr: (String, String) -> String = { es, en -> if (lang.code == "es") es else en }
    var screen by remember { mutableStateOf(if (prefs.getBoolean("onboarded", false)) "home" else "welcome") }
    var showLangs by remember { mutableStateOf(false) }
    var warn by remember { mutableStateOf<String?>(null) }
    var root by remember { mutableStateOf<Boolean?>(null) }
    val enabled = remember {
        mutableStateMapOf<String, Boolean>().also { m -> MODS.forEach { m[it.id] = prefs.getBoolean("mod_" + it.id, false) } }
    }

    LaunchedEffect(screen) { if (screen == "home" && root == null) root = checkRoot() }
    BackHandler(enabled = showLangs || warn != null || screen == "credits_view") {
        if (showLangs) showLangs = false else if (warn != null) warn = null else screen = "home"
    }

    val t = rememberInfiniteTransition(label = "bg")
    val drift by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), label = "drift"
    )

    val glass: G = { r: Dp ->
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(r) },
            effects = {
                vibrancy()
                blur(3.dp.toPx())
                lens(10.dp.toPx(), 20.dp.toPx())
            }
        )
    }

    val onToggle: (Mod) -> Unit = { m ->
        val on = enabled[m.id] == true
        val other = if (!on) MODS.firstOrNull { it.id in m.conflicts && enabled[it.id] == true } else null
        if (other != null) {
            warn = tr(
                "Para activar «${m.es}» primero desactiva «${other.es}». No pueden estar activados a la vez.",
                "To enable \"${m.en}\" first turn off \"${other.en}\". They can't be on at the same time."
            )
        } else {
            enabled[m.id] = !on
            prefs.edit().putBoolean("mod_" + m.id, !on).apply()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Box(
                Modifier.offset(x = (-80 + 60 * drift).dp, y = (60 + 40 * drift).dp).size(340.dp)
                    .background(Brush.radialGradient(listOf(Color(0x556C5CFF), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = (70 - 50 * drift).dp, y = (70 - 40 * drift).dp).size(380.dp)
                    .background(Brush.radialGradient(listOf(Color(0x3300B8FF), Color.Transparent)), CircleShape)
            )
        }

        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Crossfade(screen, animationSpec = tween(350), label = "screen") { s ->
                when (s) {
                    "welcome" -> Welcome(lang, glass, { showLangs = true }) { screen = "perms" }
                    "perms" -> Permissions(tr, glass, root, { root = it }) { screen = "credits" }
                    "credits" -> Credits(tr, glass, true) {
                        prefs.edit().putBoolean("onboarded", true).apply(); screen = "home"
                    }
                    "credits_view" -> Credits(tr, glass, false) { screen = "home" }
                    else -> Home(
                        tr, glass, enabled, root, lang.name, onToggle,
                        { showLangs = true }, { screen = "credits_view" },
                        { prefs.edit().putBoolean("onboarded", false).apply(); screen = "welcome" }
                    )
                }
            }
        }

        if (showLangs) {
            Overlay(glass, { showLangs = false }) {
                LazyColumn(Modifier.heightIn(max = 440.dp).padding(vertical = 12.dp)) {
                    items(LANGS) { l ->
                        Tx(
                            (if (l.code == lang.code) "✓   " else "      ") + l.name, 18, FontWeight.Light,
                            modifier = Modifier.fillMaxWidth().clickable {
                                langIdx = LANGS.indexOf(l)
                                prefs.edit().putInt("lang", langIdx).apply()
                                showLangs = false
                            }.padding(horizontal = 24.dp, vertical = 14.dp)
                        )
                    }
                }
            }
        }

        warn?.let { msg ->
            Overlay(glass, { warn = null }) {
                Column(Modifier.padding(24.dp)) {
                    Tx(msg, 16, FontWeight.Light)
                    Spacer(Modifier.height(20.dp))
                    Tx("OK", 16, FontWeight.Normal, sp = 1f, modifier = Modifier.clickable { warn = null }.padding(8.dp))
                }
            }
        }
    }
}
