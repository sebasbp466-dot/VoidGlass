package com.voidglass.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import kotlin.math.roundToInt

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    val glass = LocalGlass.current
    val cs = MaterialTheme.colorScheme
    Box(
        modifier.height(54.dp).bounceClick(onClick).then(glass(27.dp)).padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Tx(text, 16, FontWeight.Normal, sp = 0.5f, color = if (primary) cs.primary else cs.onSurface)
    }
}

@Composable
fun GCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val glass = LocalGlass.current
    val m = if (onClick != null) modifier.bounceClick(onClick) else modifier
    Column(m.fillMaxWidth().then(glass(28.dp)).padding(20.dp), content = content)
}

@Composable
fun Badge(text: String, color: Color, onClick: (() -> Unit)? = null) {
    val m = if (onClick != null) Modifier.bounceClick(onClick) else Modifier
    Box(
        m.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) { Tx(text, 12, FontWeight.Medium, sp = 0.5f, color = color) }
}

@Composable
fun GSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val lb = rememberLayerBackdrop()
    val pos by animateFloatAsState(if (on) 1f else 0f, spring(0.6f, 450f), label = "sw")
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 1.22f else 1f, spring(0.5f, 500f), label = "swsc")
    val track = lerp(cs.onSurface.copy(alpha = 0.18f), cs.primary, pos)
    Box(
        Modifier.width(60.dp).height(34.dp)
            .clickable(interactionSource = src, indication = null) { onChange(!on) }
    ) {
        Box(Modifier.fillMaxSize().layerBackdrop(lb).background(track, CircleShape))
        Box(
            Modifier.offset(x = (3 + 26 * pos).dp, y = 3.dp).size(28.dp)
                .graphicsLayer { scaleX = sc; scaleY = sc }
                .drawBackdrop(
                    backdrop = lb,
                    shape = { CircleShape },
                    effects = {
                        blur(1.dp.toPx())
                        lens(5.dp.toPx(), 9.dp.toPx())
                    },
                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.95f)) }
                )
        )
    }
}

@Composable
fun GSlider(value: Float, max: Float, onChange: (Float) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val lb = rememberLayerBackdrop()
    var dragging by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (dragging) 1.25f else 1f, spring(0.5f, 500f), label = "sl")
    val thumbPx = with(LocalDensity.current) { 40.dp.toPx() }
    BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp)) {
        val wPx = constraints.maxWidth.toFloat()
        val f = (value / max).coerceIn(0f, 1f)
        fun setFrom(x: Float) {
            onChange(((x - thumbPx / 2) / (wPx - thumbPx)).coerceIn(0f, 1f) * max)
        }
        Box(
            Modifier.fillMaxSize()
                .pointerInput(wPx, max) {
                    detectTapGestures(onPress = { o ->
                        dragging = true; setFrom(o.x); tryAwaitRelease(); dragging = false
                    })
                }
                .pointerInput(wPx, max) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, _ -> change.consume(); setFrom(change.position.x) }
                }
        ) {
            Box(Modifier.fillMaxWidth().height(8.dp).align(Alignment.Center).layerBackdrop(lb)) {
                Box(Modifier.fillMaxSize().background(cs.onSurface.copy(alpha = 0.14f), CircleShape))
                Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(cs.primary, CircleShape))
            }
            Box(
                Modifier.align(Alignment.CenterStart)
                    .offset { IntOffset((f * (wPx - thumbPx)).roundToInt(), 0) }
                    .size(width = 40.dp, height = 26.dp)
                    .graphicsLayer { scaleX = sc; scaleY = sc }
                    .drawBackdrop(
                        backdrop = lb,
                        shape = { CircleShape },
                        effects = {
                            blur(0.5f.dp.toPx())
                            lens(6.dp.toPx(), 12.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = 0.92f)) }
                    )
            )
        }
    }
}

@Composable
fun GSegmented(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val glass = LocalGlass.current
    val cs = MaterialTheme.colorScheme
    val idx by animateFloatAsState(selected.toFloat(), spring(0.7f, 420f), label = "seg")
    BoxWithConstraints(Modifier.fillMaxWidth().height(50.dp).then(glass(25.dp))) {
        val w = maxWidth / items.size
        Box(
            Modifier.offset(x = w * idx).width(w).fillMaxHeight().padding(5.dp)
                .clip(CircleShape).background(cs.primary.copy(alpha = 0.2f))
        )
        Row(Modifier.matchParentSize()) {
            items.forEachIndexed { i, s ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Tx(s, 14, if (i == selected) FontWeight.Normal else FontWeight.Light, a = if (i == selected) 1f else 0.6f)
                }
            }
        }
    }
}

@Composable
fun GTabs(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val glass = LocalGlass.current
    val cs = MaterialTheme.colorScheme
    val idx by animateFloatAsState(selected.toFloat(), spring(0.65f, 350f), label = "tab")
    BoxWithConstraints(
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth().height(64.dp).then(glass(32.dp))
    ) {
        val w = maxWidth / items.size
        Box(
            Modifier.offset(x = w * idx).width(w).fillMaxHeight().padding(5.dp)
                .clip(CircleShape).background(cs.primary.copy(alpha = 0.18f))
        )
        Row(Modifier.matchParentSize()) {
            items.forEachIndexed { i, s ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null
                    ) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Tx(s, 13, if (i == selected) FontWeight.Medium else FontWeight.Light, a = if (i == selected) 1f else 0.6f, sp = 0.5f)
                }
            }
        }
    }
}

@Composable
fun Overlay(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val glass = LocalGlass.current
    val cs = MaterialTheme.colorScheme
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by animateFloatAsState(if (shown) 1f else 0f, spring(0.75f, 300f), label = "ov")
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f * p.coerceIn(0f, 1f))).clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.padding(32.dp).fillMaxWidth()
                .graphicsLayer { alpha = p.coerceIn(0f, 1f); scaleX = 0.9f + 0.1f * p; scaleY = 0.9f + 0.1f * p }
                .then(glass(32.dp))
                .background(cs.surface.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
        ) { content() }
    }
}
