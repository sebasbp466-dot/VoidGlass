package com.voidglass.app

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = AppState(getSharedPreferences("voidglass", 0))
        setContent { VoidApp(app, tick.intValue) }
    }
    override fun onResume() {
        super.onResume()
        tick.intValue++
    }
}

@Composable
fun VoidApp(app: AppState, tick: Int) {
    val sysDark = isSystemInDarkTheme()
    val dark = when (app.theme) { 1 -> false; 2 -> true; else -> sysDark }
    val ctx = LocalContext.current
    val cs = if (Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { w ->
            val c = WindowCompat.getInsetsController(w, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = cs) {
        CompositionLocalProvider(LocalApp provides app, LocalDark provides dark) { AppRoot(tick) }
    }
}

@Composable
fun HomeShell(tick: Int, tab: Int, onTab: (Int) -> Unit, onPerms: () -> Unit, onCredits: () -> Unit, onReplay: () -> Unit) {
    val app = LocalApp.current
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(300)) + slideInHorizontally(tween(300)) { dir * it / 8 }) togetherWith
                        (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -dir * it / 8 })
                },
                label = "tab"
            ) { t ->
                when (t) {
                    0 -> HomeTab(tick, onPerms)
                    1 -> StyleTab()
                    2 -> ModulesTab()
                    else -> SettingsTab(tick, onPerms, onCredits, onReplay)
                }
            }
        }
        GTabs(
            listOf(app.tr("Inicio", "Home"), app.tr("Estilo", "Style"), app.tr("Módulos", "Modules"), app.tr("Ajustes", "Settings")),
            tab, onTab
        )
    }
}

@Composable
fun AppRoot(tick: Int) {
    val app = LocalApp.current
    val dark = LocalDark.current
    val cs = MaterialTheme.colorScheme
    val backdrop = rememberLayerBackdrop()
    var screen by remember { mutableStateOf(if (app.onboarded) "home" else "welcome") }
    var tab by remember { mutableStateOf(0) }

    LaunchedEffect(tick, screen) {
        if (screen == "home" || screen == "perms" || screen == "perms_onb") {
            app.root = checkRoot()
            app.lsposed = if (app.root == true) checkLsposed() else false
        }
    }
    BackHandler(enabled = app.showLangs || app.dialog != null || screen == "perms" || screen == "credits") {
        if (app.showLangs) app.showLangs = false
        else if (app.dialog != null) app.dialog = null
        else screen = "home"
    }

    val t = rememberInfiniteTransition(label = "bg")
    val d by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    val tint = tintFor(dark, app.tint)
    val glass: G = { r: Dp ->
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(r) },
            effects = {
                vibrancy()
                blur(app.blur.coerceAtLeast(0.5f).dp.toPx())
                lens(app.lens.coerceAtLeast(1f).dp.toPx(), (app.lens * 2f).coerceAtLeast(2f).dp.toPx())
            },
            onDrawSurface = { drawRect(tint) }
        )
    }
    val glowA = if (dark) 0.35f else 0.30f

    Box(Modifier.fillMaxSize().background(cs.background)) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Box(
                Modifier.offset(x = (-90 + 80 * d).dp, y = (40 + 50 * d).dp).size(380.dp)
                    .background(Brush.radialGradient(listOf(cs.primary.copy(alpha = glowA), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = (80 - 60 * d).dp, y = (70 - 50 * d).dp).size(400.dp)
                    .background(Brush.radialGradient(listOf(cs.tertiary.copy(alpha = glowA), Color.Transparent)), CircleShape)
            )
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = (-120 + 60 * d).dp, y = (100 - 80 * d).dp).size(300.dp)
                    .background(Brush.radialGradient(listOf(cs.secondary.copy(alpha = glowA * 0.8f), Color.Transparent)), CircleShape)
            )
        }

        CompositionLocalProvider(LocalGlass provides glass) {
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 24 }) togetherWith
                            (fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 24 })
                    },
                    label = "screen"
                ) { s ->
                    when (s) {
                        "welcome" -> Welcome({ app.showLangs = true }) { screen = "perms_onb" }
                        "perms_onb" -> PermissionsScreen(tick, true) { screen = "credits_onb" }
                        "credits_onb" -> Credits(true) { app.onboarded = true; app.save(); screen = "home" }
                        "perms" -> PermissionsScreen(tick, false) { screen = "home" }
                        "credits" -> Credits(false) { screen = "home" }
                        else -> HomeShell(
                            tick, tab, { tab = it }, { screen = "perms" }, { screen = "credits" },
                            { app.onboarded = false; app.save(); screen = "welcome" }
                        )
                    }
                }
            }

            if (app.showLangs) {
                Overlay({ app.showLangs = false }) {
                    LazyColumn(Modifier.heightIn(max = 440.dp).padding(vertical = 12.dp)) {
                        items(LANGS) { l ->
                            Tx(
                                (if (l.code == app.lang.code) "✓   " else "      ") + l.name, 18,
                                modifier = Modifier.fillMaxWidth().bounceClick {
                                    app.langIdx = LANGS.indexOf(l); app.save(); app.showLangs = false
                                }.padding(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }

            app.dialog?.let { dlg ->
                Overlay({ app.dialog = null }) {
                    Column(Modifier.padding(24.dp)) {
                        Tx(dlg.msg, 16)
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (dlg.action != null) {
                                Tx(
                                    app.tr("Cancelar", "Cancel"), 15, a = 0.7f,
                                    modifier = Modifier.bounceClick { app.dialog = null }.padding(12.dp)
                                )
                                Tx(
                                    dlg.actionLabel, 15, FontWeight.Medium, color = cs.primary,
                                    modifier = Modifier.bounceClick { app.dialog = null; dlg.action.invoke() }.padding(12.dp)
                                )
                            } else {
                                Tx(
                                    "OK", 15, FontWeight.Medium, color = cs.primary,
                                    modifier = Modifier.bounceClick { app.dialog = null }.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
