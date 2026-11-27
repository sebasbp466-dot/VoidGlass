package com.voidglass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlinx.coroutines.delay

data class Lang(val name: String, val hello: String, val cont: String)

val LANGS = listOf(
    Lang("Español", "Hola", "Continuar"),
    Lang("English", "Hello", "Continue"),
    Lang("Português", "Olá", "Continuar"),
    Lang("Français", "Bonjour", "Continuer"),
    Lang("Deutsch", "Hallo", "Weiter"),
    Lang("Italiano", "Ciao", "Continua"),
    Lang("日本語", "こんにちは", "続ける"),
    Lang("한국어", "안녕하세요", "계속"),
    Lang("中文", "你好", "继续"),
    Lang("Русский", "Привет", "Продолжить"),
    Lang("العربية", "مرحبا", "متابعة"),
    Lang("हिन्दी", "नमस्ते", "जारी रखें"),
    Lang("Türkçe", "Merhaba", "Devam"),
    Lang("Nederlands", "Hallo", "Doorgaan"),
    Lang("Polski", "Cześć", "Dalej"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VoidApp() }
    }
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
                style = Stroke(2.dp.toPx())
            )
        }
        drawCircle(Color.Black, r, c)
        drawCircle(Color(0xFFF2F2FF), r, c, style = Stroke(2.5.dp.toPx()))
        drawCircle(Color(0x40FFFFFF), r * 1.3f, c, style = Stroke(1.dp.toPx()))
    }
}

@Composable
fun GlassButton(text: String, glass: Modifier, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(56.dp).then(glass).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 17.sp)
    }
}

@Composable
fun VoidApp() {
    val backdrop = rememberLayerBackdrop()
    var lang by remember { mutableStateOf(LANGS[0]) }
    var showLangs by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(0) }
    var intro by remember { mutableStateOf(true) }
    var introHello by remember { mutableStateOf(LANGS[0].hello) }

    LaunchedEffect(Unit) {
        for (l in LANGS) { introHello = l.hello; delay(550) }
        intro = false
    }

    val t = rememberInfiniteTransition(label = "bg")
    val drift by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift"
    )

    val glass = { r: Dp ->
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { RoundedCornerShape(r) },
            effects = {
                vibrancy()
                blur(4.dp.toPx())
                lens(12.dp.toPx(), 24.dp.toPx())
            }
        )
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F))) {
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            Box(Modifier.offset((30 + 70 * drift).dp, (120 + 40 * drift).dp).size(200.dp).background(Color(0xFF6C4DFF), CircleShape))
            Box(Modifier.offset((180 - 70 * drift).dp, (500 - 60 * drift).dp).size(230.dp).background(Color(0xFF00C2FF), CircleShape))
        }

        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))
            if (step == 0) {
                BlackHole(Modifier.size(120.dp))
                Spacer(Modifier.height(28.dp))
                Crossfade(
                    targetState = if (intro) introHello else lang.hello,
                    animationSpec = tween(450),
                    label = "hello"
                ) { Text(it, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light) }
                Spacer(Modifier.weight(1f))
                GlassButton("🌐  " + lang.name, glass(28.dp)) { showLangs = true }
                Spacer(Modifier.height(12.dp))
                GlassButton(lang.cont, glass(28.dp)) { step = 1 }
            } else {
                Text("Siguiente: permisos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.weight(1f))
            }
        }

        if (showLangs) {
            Box(
                Modifier.fillMaxSize().background(Color(0x88000000)).clickable { showLangs = false },
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.padding(32.dp).fillMaxWidth().heightIn(max = 460.dp).then(glass(32.dp))) {
                    LazyColumn(Modifier.padding(vertical = 12.dp)) {
                        items(LANGS) { l ->
                            Text(
                                (if (l == lang) "✓   " else "      ") + l.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { lang = l; intro = false; showLangs = false }
                                    .padding(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
