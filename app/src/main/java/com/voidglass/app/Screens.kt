package com.voidglass.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Welcome(onLang: () -> Unit, onNext: () -> Unit) {
    val app = LocalApp.current
    var intro by remember { mutableStateOf(true) }
    var hello by remember { mutableStateOf(LANGS[0].hello) }
    LaunchedEffect(Unit) {
        for (l in LANGS) { hello = l.hello; delay(550) }
        intro = false
    }
    LaunchedEffect(app.lang.code) { if (app.lang.code != "es") intro = false }
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Tx(app.lang.name, 14, a = 0.6f, sp = 1f, modifier = Modifier.bounceClick(onLang).padding(10.dp))
        }
        Spacer(Modifier.weight(1f))
        BlackHole(Modifier.size(170.dp))
        Spacer(Modifier.height(24.dp))
        AnimatedContent(
            targetState = if (intro) hello else app.lang.hello,
            transitionSpec = {
                (fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }) togetherWith
                    (fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 3 })
            },
            label = "hello"
        ) { Tx(it, 46, FontWeight.Thin, sp = 2f) }
        Spacer(Modifier.height(8.dp))
        Tx("VoidGlass", 13, a = 0.5f, sp = 5f)
        Spacer(Modifier.weight(1f))
        Pill(app.lang.cont, Modifier.fillMaxWidth(), true, onNext)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun PermCard(p: PermItem, onAct: () -> Unit) {
    val app = LocalApp.current
    val cs = MaterialTheme.colorScheme
    GCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Tx(app.tr(p.es, p.en), 17)
                Spacer(Modifier.height(4.dp))
                Tx(app.tr(p.dEs, p.dEn), 12, a = 0.55f)
            }
            if (p.ok) Badge(app.tr("Activado", "Enabled"), cs.primary)
            else Badge(app.tr("Activar", "Enable"), cs.error, onAct)
        }
    }
}

@Composable
fun PermissionsScreen(tick: Int, onboarding: Boolean, onDone: () -> Unit) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    var local by remember { mutableStateOf(0) }
    val items = remember(tick, local, app.root) { permItems(ctx, app.root) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { local++ }
    val ok = items.count { it.ok }
    val prog by animateFloatAsState(ok / items.size.toFloat(), tween(700), label = "pg")

    fun recheck() {
        scope.launch {
            app.root = checkRoot()
            app.lsposed = if (app.root == true) checkLsposed() else false
            local++
        }
    }

    fun act(id: String) {
        if (id == "root") recheck()
        else if (id == "notif" && Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else openPerm(ctx, id)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(28.dp))
            Tx(app.tr("Permisos", "Permissions"), 36, FontWeight.Thin, sp = 2f)
            Spacer(Modifier.height(6.dp))
            Tx(
                if (ok == items.size) app.tr("Todo listo.", "All set.")
                else app.tr("$ok de ${items.size} listos · faltan ${items.size - ok}", "$ok of ${items.size} ready · ${items.size - ok} missing"),
                14, a = 0.6f
            )
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(cs.onSurface.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(prog).background(cs.primary, CircleShape))
            }
            Spacer(Modifier.height(24.dp))
            if (app.root == false) {
                Appear(0) {
                    Column {
                        GCard {
                            Tx(app.tr("Sin root", "No root"), 16, FontWeight.Medium, color = cs.error)
                            Spacer(Modifier.height(4.dp))
                            Tx(
                                app.tr(
                                    "No detecto acceso root. Sin él, casi todas las funciones no podrán aplicarse al sistema.",
                                    "Root access not detected. Without it, most features can't be applied to the system."
                                ), 13, a = 0.7f
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
            items.forEachIndexed { i, p ->
                Appear(i + 1) { Column { PermCard(p) { act(p.id) }; Spacer(Modifier.height(14.dp)) } }
            }
            Appear(items.size + 1) {
                GCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Tx("LSPosed", 17)
                            Spacer(Modifier.height(4.dp))
                            Tx(
                                app.tr(
                                    "Se detecta buscando el módulo en el gestor de root. Puede no detectarse según tu instalación.",
                                    "Detected by looking for the module in your root manager. It may not be detected depending on your setup."
                                ), 12, a = 0.55f
                            )
                        }
                        when (app.lsposed) {
                            true -> Badge(app.tr("Detectado", "Detected"), cs.primary)
                            false -> Badge(app.tr("No detectado", "Not detected"), cs.error)
                            null -> Badge("…", cs.onSurface)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        Pill(app.tr("Verificar de nuevo", "Check again"), Modifier.fillMaxWidth()) { recheck() }
        Spacer(Modifier.height(10.dp))
        Pill(
            if (onboarding) app.tr("Continuar", "Continue") else app.tr("Listo", "Done"),
            Modifier.fillMaxWidth(), true, onDone
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun CreditRow(name: String, project: String, desc: String) {
    Column(Modifier.padding(bottom = 22.dp)) {
        Tx(name, 19)
        if (project.isNotEmpty()) Tx(project, 13, a = 0.6f, sp = 0.5f)
        Spacer(Modifier.height(2.dp))
        Tx(desc, 13, a = 0.45f)
    }
}

@Composable
fun Credits(last: Boolean, onDone: () -> Unit) {
    val app = LocalApp.current
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(28.dp))
            Tx(app.tr("Gracias", "Thanks"), 36, FontWeight.Thin, sp = 2f)
            Spacer(Modifier.height(6.dp))
            Tx(app.tr("VoidGlass existe gracias al trabajo de:", "VoidGlass exists thanks to the work of:"), 14, a = 0.6f)
            Spacer(Modifier.height(32.dp))
            CreditRow("Kyant0", "AndroidLiquidGlass",
                app.tr("El efecto liquid glass de toda esta app", "The liquid glass effect of this whole app"))
            CreditRow("Mahmud0808 (DrDisagree)", "GlassLayer · XMorphClock",
                app.tr("Liquid glass de sistema y relojes de bloqueo", "System liquid glass and lockscreen clocks"))
            CreditRow("LSPosed", "Xposed",
                app.tr("La base para modificar el sistema", "The base for modifying the system"))
            CreditRow("LineageOS", "",
                app.tr("El sistema donde nace todo esto", "The system where all this is born"))
            CreditRow("Material Design", "Google",
                app.tr("Los colores dinámicos de la interfaz", "The dynamic colors of the interface"))
            Spacer(Modifier.height(6.dp))
            GCard {
                Tx(app.tr("Beta temprana", "Early beta"), 17, FontWeight.Normal, sp = 1f)
                Spacer(Modifier.height(6.dp))
                Tx(
                    app.tr(
                        "Probada solo en Redmi 9 (Lancelot) con LineageOS 24.0, Android 17. En otros dispositivos puede fallar.",
                        "Tested only on Redmi 9 (Lancelot) with LineageOS 24.0, Android 17. It may fail on other devices."
                    ), 13, a = 0.65f
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        Pill(if (last) app.tr("Entrar", "Enter") else app.tr("Volver", "Back"), Modifier.fillMaxWidth(), true, onDone)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun StatusRow(label: String, state: Boolean?, good: String, bad: String, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    val c = when (state) { true -> cs.primary; false -> cs.error; null -> cs.onSurface.copy(alpha = 0.4f) }
    val m = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(m.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(c, CircleShape))
        Spacer(Modifier.width(12.dp))
        Tx(label, 16, modifier = Modifier.weight(1f))
        Tx(when (state) { true -> good; false -> bad; null -> "…" }, 14, a = 0.8f, color = c)
    }
}

@Composable
fun HomeTab(tick: Int, onPerms: () -> Unit) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val items = remember(tick, app.root) { permItems(ctx, app.root) }
    val missing = items.count { !it.ok }
    val active = MODS.filter { app.mods[it.id] == true }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BlackHole(Modifier.size(150.dp)) }
        Tx("VoidGlass", 36, FontWeight.Thin, sp = 3f, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        Tx(
            "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}", 12, a = 0.55f, sp = 0.5f,
            modifier = Modifier.fillMaxWidth(), align = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        if (missing > 0 || app.root == false) {
            Appear(0) {
                Column {
                    GCard(onClick = onPerms) {
                        Tx(app.tr("Faltan cosas por activar", "Some things need enabling"), 17, FontWeight.Medium, color = cs.error)
                        Spacer(Modifier.height(4.dp))
                        Tx(
                            app.tr("$missing permisos pendientes. Toca para revisarlos.", "$missing pending permissions. Tap to review them."),
                            13, a = 0.7f
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
        Appear(1) {
            Column {
                GCard {
                    Tx(app.tr("Estado", "Status"), 12, FontWeight.Medium, a = 0.5f, sp = 3f)
                    Spacer(Modifier.height(6.dp))
                    StatusRow("Root", app.root, app.tr("Activo", "Active"), app.tr("No detectado", "Not detected"))
                    StatusRow("LSPosed", app.lsposed, app.tr("Detectado", "Detected"), app.tr("No detectado", "Not detected"))
                    StatusRow(
                        app.tr("Permisos", "Permissions"), missing == 0,
                        app.tr("Todos listos", "All ready"), app.tr("Faltan $missing", "$missing missing"), onPerms
                    )
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        Appear(2) {
            Column {
                GCard {
                    Tx(app.tr("Módulos activos", "Active modules"), 12, FontWeight.Medium, a = 0.5f, sp = 3f)
                    Spacer(Modifier.height(10.dp))
                    if (active.isEmpty()) Tx(app.tr("Ninguno todavía. Actívalos en Módulos.", "None yet. Enable them in Modules."), 14, a = 0.6f)
                    else active.forEach { Tx("•  " + app.tr(it.es, it.en), 15, modifier = Modifier.padding(vertical = 3.dp)) }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        Appear(3) {
            Column {
                Pill(app.tr("Verificar de nuevo", "Check again"), Modifier.fillMaxWidth()) {
                    scope.launch {
                        app.root = checkRoot()
                        app.lsposed = if (app.root == true) checkLsposed() else false
                    }
                }
                Spacer(Modifier.height(10.dp))
                Pill(app.tr("Reiniciar System UI", "Restart System UI"), Modifier.fillMaxWidth()) {
                    app.dialog = if (app.root == true) Dlg(
                        app.tr("La barra de estado y los paneles se reiniciarán unos segundos.", "The status bar and panels will restart for a few seconds."),
                        {
                            scope.launch(Dispatchers.IO) {
                                runRoot("pkill -f com.android.systemui || killall com.android.systemui || am crash com.android.systemui")
                            }
                        },
                        app.tr("Reiniciar", "Restart")
                    ) else Dlg(app.tr("Necesitas acceso root para esto.", "You need root access for this."))
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun GlassPreview() {
    val app = LocalApp.current
    val dark = LocalDark.current
    val cs = MaterialTheme.colorScheme
    val pb = rememberLayerBackdrop()
    val t = rememberInfiniteTransition(label = "pv")
    val d by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d")
    val tint = tintFor(dark, app.tint)
    Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(32.dp)).background(cs.surfaceVariant.copy(alpha = 0.5f))) {
        Box(Modifier.fillMaxSize().layerBackdrop(pb)) {
            Box(Modifier.offset(x = (20 + 130 * d).dp, y = 20.dp).size(110.dp).background(cs.primary, CircleShape))
            Box(Modifier.offset(x = (200 - 130 * d).dp, y = 90.dp).size(90.dp).background(cs.tertiary, CircleShape))
            Box(Modifier.offset(x = 110.dp, y = (130 - 40 * d).dp).size(70.dp).background(cs.secondary, CircleShape))
        }
        Box(
            Modifier.align(Alignment.Center).height(60.dp).width(200.dp).drawBackdrop(
                backdrop = pb,
                shape = { RoundedCornerShape(30.dp) },
                effects = {
                    vibrancy()
                    blur(app.blur.coerceAtLeast(0.5f).dp.toPx())
                    lens(app.lens.coerceAtLeast(1f).dp.toPx(), (app.lens * 2f).coerceAtLeast(2f).dp.toPx())
                },
                onDrawSurface = { drawRect(tint) }
            ),
            contentAlignment = Alignment.Center
        ) { Tx("Liquid Glass", 18) }
    }
}

@Composable
fun StyleTab() {
    val app = LocalApp.current
    val hap = LocalHapticFeedback.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Tx(app.tr("Estilo", "Style"), 34, FontWeight.Thin, sp = 2f)
        Spacer(Modifier.height(18.dp))
        Appear(0) { Column { GlassPreview(); Spacer(Modifier.height(18.dp)) } }
        Appear(1) {
            Column {
                Tx(app.tr("Tema", "Theme"), 12, FontWeight.Medium, a = 0.5f, sp = 3f)
                Spacer(Modifier.height(10.dp))
                GSegmented(listOf(app.tr("Sistema", "System"), app.tr("Claro", "Light"), app.tr("Oscuro", "Dark")), app.theme) {
                    app.theme = it; app.save()
                }
                Spacer(Modifier.height(6.dp))
                Tx(
                    if (Build.VERSION.SDK_INT >= 31) app.tr("Colores Material You según tu fondo de pantalla.", "Material You colors from your wallpaper.")
                    else app.tr("Colores Material You no disponibles en este Android.", "Material You colors unavailable on this Android."),
                    12, a = 0.45f, modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                )
                Spacer(Modifier.height(18.dp))
            }
        }
        Appear(2) {
            Column {
                GCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Tx(app.tr("Desenfoque", "Blur"), 16); Tx("%.0f".format(app.blur), 14, a = 0.6f)
                    }
                    GSlider(app.blur, 20f) { app.blur = it; app.save() }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Tx(app.tr("Refracción", "Refraction"), 16); Tx("%.0f".format(app.lens), 14, a = 0.6f)
                    }
                    GSlider(app.lens, 40f) { app.lens = it; app.save() }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Tx(app.tr("Tinte del cristal", "Glass tint"), 16); Tx("%.0f%%".format(app.tint * 100), 14, a = 0.6f)
                    }
                    GSlider(app.tint, 1f) { app.tint = it; app.save() }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
        Appear(3) {
            Column {
                GCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Tx(app.tr("Vibración háptica", "Haptic feedback"), 16)
                            Tx(app.tr("Al activar interruptores", "When toggling switches"), 12, a = 0.5f)
                        }
                        GSwitch(app.haptics) {
                            app.haptics = it; app.save()
                            if (it) hap.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Pill(app.tr("Restablecer apariencia", "Reset appearance"), Modifier.fillMaxWidth()) {
                    app.blur = 4f; app.lens = 16f; app.tint = 0.5f; app.theme = 0; app.save()
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ModCard(m: Mod) {
    val app = LocalApp.current
    val cs = MaterialTheme.colorScheme
    val hap = LocalHapticFeedback.current
    val on = app.mods[m.id] == true
    val miss = buildList {
        if (m.needsRoot && app.root != true) add(app.tr("Falta root", "Root missing"))
        if (m.needsXposed && app.lsposed != true) add(app.tr("LSPosed no detectado", "LSPosed not detected"))
    }
    fun change(want: Boolean) {
        if (want) {
            val other = MODS.firstOrNull { it.id in m.conflicts && app.mods[it.id] == true }
            if (m.needsRoot && app.root != true) {
                app.dialog = Dlg(app.tr("«${m.es}» necesita acceso root. Concédelo en Permisos.", "\"${m.en}\" needs root access. Grant it in Permissions."))
            } else if (other != null) {
                app.dialog = Dlg(
                    app.tr(
                        "Para activar «${m.es}» primero desactiva «${other.es}». No pueden estar activados a la vez.",
                        "To enable \"${m.en}\" first turn off \"${other.en}\". They can't be on at the same time."
                    )
                )
            } else {
                app.mods[m.id] = true; app.save()
                if (app.haptics) hap.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            app.mods[m.id] = false; app.save()
            if (app.haptics) hap.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    GCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 14.dp)) {
                Tx(app.tr(m.es, m.en), 17)
                Spacer(Modifier.height(4.dp))
                Tx(app.tr(m.dEs, m.dEn), 12, a = 0.55f)
                Spacer(Modifier.height(8.dp))
                if (miss.isEmpty()) Tx("✓ " + app.tr("Requisitos listos", "Requirements met"), 12, FontWeight.Medium, color = cs.primary)
                else Tx(miss.joinToString(" · "), 12, FontWeight.Medium, color = cs.error)
            }
            GSwitch(on) { change(it) }
        }
    }
}

@Composable
fun ModulesTab() {
    val app = LocalApp.current
    var n = 0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Tx(app.tr("Módulos", "Modules"), 34, FontWeight.Thin, sp = 2f)
        Spacer(Modifier.height(4.dp))
        Tx(app.tr("Aún no aplican cambios reales al sistema.", "They don't apply real system changes yet."), 13, a = 0.5f)
        Spacer(Modifier.height(20.dp))
        GROUPS.forEach { (gid, es, en) ->
            Tx(app.tr(es, en).uppercase(), 11, FontWeight.Medium, a = 0.5f, sp = 3f, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
            MODS.filter { it.group == gid }.forEach { m ->
                val i = n++
                Appear(i) { Column { ModCard(m); Spacer(Modifier.height(14.dp)) } }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun SettingRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) Modifier.bounceClick(onClick) else Modifier
    Row(m.fillMaxWidth().padding(vertical = 18.dp, horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Tx(label, 17)
        Tx(value, 14, a = 0.5f)
    }
}

@Composable
fun SettingsTab(tick: Int, onPerms: () -> Unit, onCredits: () -> Unit, onReplay: () -> Unit) {
    val app = LocalApp.current
    val ctx = LocalContext.current
    val items = remember(tick, app.root) { permItems(ctx, app.root) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Tx(app.tr("Ajustes", "Settings"), 34, FontWeight.Thin, sp = 2f)
        Spacer(Modifier.height(18.dp))
        Appear(0) {
            GCard {
                SettingRow(app.tr("Idioma", "Language"), app.lang.name) { app.showLangs = true }
                SettingRow(app.tr("Permisos", "Permissions"), "${items.count { it.ok }} / ${items.size}", onPerms)
                SettingRow(app.tr("Créditos", "Credits"), "", onCredits)
                SettingRow(app.tr("Repetir bienvenida", "Replay welcome"), "", onReplay)
            }
        }
        Spacer(Modifier.height(14.dp))
        Appear(1) {
            GCard {
                SettingRow(app.tr("Versión", "Version"), "0.1 beta")
                SettingRow(app.tr("Probada en", "Tested on"), "Redmi 9 · LineageOS 24.0")
                SettingRow("Android", Build.VERSION.RELEASE)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
